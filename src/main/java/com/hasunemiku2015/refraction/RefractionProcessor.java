package com.hasunemiku2015.refraction;

import com.google.auto.service.AutoService;
import com.hasunemiku2015.refraction.annotations.Abstracted;
import com.hasunemiku2015.refraction.annotations.BaseClass;
import com.hasunemiku2015.refraction.annotations.Field;
import com.squareup.javapoet.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@SupportedAnnotationTypes("com.hasunemiku2015.refraction.annotations.BaseClass")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class RefractionProcessor extends AbstractProcessor {
    private Filer filer;
    private Types typeUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        filer = processingEnv.getFiler();
        typeUtils = processingEnv.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);

            for (Element element : annotatedElements) {
                if (element.getKind() == ElementKind.INTERFACE) {
                    String defaultBaseClassName = element.getAnnotation(BaseClass.class).name();

                    TypeName interfaceName = TypeName.get(element.asType());
                    String packageName = getPackageName(((TypeElement) element).getQualifiedName().toString()) + ".refraction.generated";
                    String generatedClassName = element.getSimpleName().toString() + "Implementation";

                    // Code generation with JavaPoet
                    TypeSpec.Builder classBuilder = TypeSpec.classBuilder(generatedClassName)
                            .addSuperinterface(element.asType())
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .addField(createBaseObjectField())
                            .addMethod(createConstructor())
                            .addMethod(createUnWrapper())
                            .addMethod(createMaker(interfaceName, generatedClassName, packageName));

                    for (Element subElement : element.getEnclosedElements()) {
                        if (!(subElement instanceof ExecutableElement)) continue;
                        ExecutableElement exeElement = (ExecutableElement) subElement;

                        if (exeElement.getAnnotation(Field.class) != null) {
                            String fieldName = exeElement.getAnnotation(Field.class).name();
                            if (exeElement.getReturnType().getKind() == TypeKind.VOID) {
                                classBuilder.addMethod(createNewFieldModifier(exeElement, fieldName,
                                        exeElement.getAnnotation(BaseClass.class) != null ?
                                                exeElement.getAnnotation(BaseClass.class).name() : defaultBaseClassName));
                            } else {
                                classBuilder.addMethod(createNewFieldAccessor(exeElement, fieldName,
                                        exeElement.getAnnotation(BaseClass.class) != null ?
                                                exeElement.getAnnotation(BaseClass.class).name() : defaultBaseClassName));
                            }
                        } else {
                            classBuilder.addMethod(createNewMethodForwarder(exeElement,
                                    exeElement.getAnnotation(BaseClass.class) != null ?
                                            exeElement.getAnnotation(BaseClass.class).name() : defaultBaseClassName));
                        }
                    }

                    JavaFile.Builder fileBuilder = JavaFile.builder(packageName, classBuilder.build())
                            .addStaticImport(ClassName.get("com.hasunemiku2015.refraction", "RefractionEnvironmentVariableStore"), "a")
                            .addFileComment("This file is generated by Refraction API.\n")
                            .addFileComment("Call the create() method directly to get a implementation of your interface.\n")
                            .addFileComment("If there is any bugs, feel free to contact the developers.");
                    try {
                        fileBuilder.build().writeTo(filer);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        return true;
    }

    private FieldSpec createBaseObjectField() {
        return FieldSpec.builder(TypeName.OBJECT, "base", Modifier.PRIVATE, Modifier.FINAL).build();
    }

    private MethodSpec createConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(TypeName.OBJECT, "object")
                .addStatement("this.base = object")
                .build();
    }

    private MethodSpec createUnWrapper() {
        return MethodSpec.methodBuilder("A")
                .returns(TypeName.OBJECT)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("return base")
                .build();
    }

    private MethodSpec createMaker(TypeName interfaceName, String generatedClassName, String packageName) {
        return MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(TypeName.OBJECT, "baseObject")
                .returns(interfaceName)
                .addStatement("return new $T(baseObject)",
                        ClassName.get(packageName, generatedClassName))
                .build();
    }

    private MethodSpec createNewFieldAccessor(ExecutableElement element, String fieldName, String baseClassFullName) {
        TypeName returnClass = TypeName.get(element.getReturnType());

        MethodSpec.Builder var = MethodSpec.methodBuilder(element.getSimpleName().toString());
        var.addModifiers(Modifier.PUBLIC).returns(returnClass);

        var.beginControlFlow("try")
                .addStatement("Class<?> clazz = Class.forName(a($S))", baseClassFullName)
                .addStatement("$T f = clazz.getDeclaredField(a($S))", ClassName.get(java.lang.reflect.Field.class), fieldName)
                .addStatement("f.setAccessible(true)")
                .addStatement("Object returnValue = f.get(base)");

        returnValueAndCatchStatement(returnClass, element, var);
        return var.build();
    }

    private MethodSpec createNewFieldModifier(ExecutableElement element, String fieldName, String baseClassFullName) {
        ParameterSpec inputParam = getInputParams(element).get(0);

        MethodSpec.Builder var = MethodSpec.methodBuilder(element.getSimpleName().toString());
        var.addModifiers(Modifier.PUBLIC)
                .addParameter(inputParam)
                .returns(TypeName.VOID)
                .beginControlFlow("try")
                .addStatement("Class<?> clazz = Class.forName(a($S))", baseClassFullName)
                .addStatement("$T f = clazz.getDeclaredField(a($S))", ClassName.get(java.lang.reflect.Field.class), fieldName)
                .addStatement("f.setAccessible(true)")
                .addStatement("f.set(base, $L)", inputParam.name)
                .endControlFlow()
                .beginControlFlow("catch (Exception ex)")
                .addStatement("ex.printStackTrace()")
                .endControlFlow();
        return var.build();
    }

    private MethodSpec createNewMethodForwarder(ExecutableElement element, String baseClassFullName) {
        TypeName returnClass = TypeName.get(element.getReturnType());

        MethodSpec.Builder var = MethodSpec.methodBuilder(element.getSimpleName().toString());
        var.addModifiers(Modifier.PUBLIC)
                .addParameters(getInputParams(element))
                .returns(returnClass)
                .beginControlFlow("try")
                .addStatement("Class<?> clazz = Class.forName(a($S))", baseClassFullName);

        StringBuilder findMethodString = new StringBuilder();
        findMethodString.append("$T mth = clazz.getDeclaredMethod(a($S)");

        // Unwrap if wrapper
        List<ParameterSpec> inputParams = getInputParams(element);
        List<String> methodInputParamNames = new ArrayList<>();

        for (int i = 0; i < element.getParameters().size(); i++) {
            ParameterSpec spec = inputParams.get(i);
            if (element.getParameters().get(i).getAnnotation(Abstracted.class) == null) {
                findMethodString.append(String.format(",%s.class", spec.type));
                methodInputParamNames.add(spec.name);
            } else {
                String varName = getRandomString(8);
                String varClsName = getRandomString(8);
                String varFieldName = getRandomString(8);

                var.addStatement("Class<?> $L = Class.forName($L)", varClsName, spec.type.toString())
                        .addStatement("$T $L = $L.getDeclaredField(\"base\")",
                                ClassName.get(java.lang.reflect.Field.class), varFieldName, varClsName)
                        .addStatement("$L.setAccessible(true)", varFieldName)
                        .addStatement("Object $L = $L.get($N)", varName, varFieldName, spec);

                findMethodString.append(String.format(
                        ",Class.forName(a((BaseClass) Class.forName(%s).getAnnotation(BaseClass.class)).name())",
                        spec.type));
                methodInputParamNames.add(varName);
            }
        }

        findMethodString.append(")");
        var.addStatement(findMethodString.toString(), ClassName.get(java.lang.reflect.Method.class),
                        element.getSimpleName().toString())
                .addStatement("mth.setAccessible(true)");

        StringBuilder invokeString = new StringBuilder();
        invokeString.append("Object returnValue = mth.invoke(base");
        methodInputParamNames.forEach((String name) -> invokeString.append(String.format(",%s", name)));
        invokeString.append(")");
        var.addStatement(invokeString.toString());

        returnValueAndCatchStatement(returnClass, element, var);
        return var.build();
    }

    private List<ParameterSpec> getInputParams(ExecutableElement var1) {
        return var1.getParameters().stream().map(variableElement -> {
            String name = variableElement.getSimpleName().toString();
            return ParameterSpec.builder(TypeName.get(variableElement.asType()), name).build();
        }).collect(Collectors.toList());
    }

    private void returnValueAndCatchStatement(TypeName returnClass, ExecutableElement element, MethodSpec.Builder var) {
        if (!(returnClass == TypeName.VOID)) {
            TypeElement var1 = (TypeElement) typeUtils.asElement(element.getReturnType());
            if (var1 != null && var1.getAnnotation(BaseClass.class) != null) {
                String packageName = getPackageName(var1.getQualifiedName().toString()) + ".refraction.generated";
                ClassName implementationLocation = ClassName.get(packageName, var1.getSimpleName() + "Implementation");
                var.addStatement("return $T.create(returnValue)", implementationLocation);
            } else {
                var.addStatement("return ($T) returnValue", returnClass);
            }
        }

        var.endControlFlow()
                .beginControlFlow("catch (Exception ex)")
                .addStatement("ex.printStackTrace()")
                .endControlFlow();

        if (!(returnClass == TypeName.VOID)) {
            if (returnClass == TypeName.INT || returnClass == TypeName.DOUBLE || returnClass == TypeName.FLOAT ||
                    returnClass == TypeName.SHORT || returnClass == TypeName.LONG || returnClass == TypeName.BYTE) {
                var.addStatement("return 0");
            } else {
                var.addStatement("return null");
            }
        }
    }

    private String getPackageName(String classFullName) {
        String packageName = "";
        int lastDot = classFullName.lastIndexOf('.');
        if (lastDot > 0) {
            packageName = classFullName.substring(0, lastDot);
        }
        return packageName;
    }

    private String getRandomString(int n) {
        // chose a Character random from this String
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "abcdefghijklmnopqrstuvxyz";

        // create StringBuffer size of AlphaNumericString
        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {

            // generate a random number between
            // 0 to AlphaNumericString variable length
            int index
                    = (int) (AlphaNumericString.length()
                    * Math.random());

            // add Character one by one in end of sb
            sb.append(AlphaNumericString
                    .charAt(index));
        }

        return sb.toString();
    }
}

