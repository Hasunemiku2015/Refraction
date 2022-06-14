# Refraction
>Refraction is a java code-generation API to allow easy use of java reflection API.

# Ｈow to Use?

### 2.1 Maven Dependency

Add the following repository to your pom.xml.
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```
Add the following dependencies to your pom.xml.
```xml
<dependency>
    <groupId>com.github.Hasunemiku2015</groupId>
    <artifactId>Refraction</artifactId>
    <version>1.0</version>
</dependency>
```

### 2.2 Implementing the code

There are 4 annotations provided by this API
```java
  @BaseClass   // This Configures the BaseClass that reflection will reflect onto
  @Field       // This signifies a method to be a field accessor
  @Constructor // This signifies a method to be a constructor
  @Abstracted  // Used in input parameters to signify an input to be a wrapper.
```

You can use the API by creating a wrapper interface. An example interface is given here, it should be self explanatory.

```java

@BaseClass("com.example.example_project.SomeSecretClass")
public interface ExampleInterface {

    // If you want to access a method, copy it from the base class.
    void doSomething();

    int getSomeInteger();


    // @Field declares a method to be a field accessor.
    // If return type is void, it is a setter or else it is a getter.
    @Field(name = "privateVariable")
    Object getPrivateVariable();

    // It could cast the returned value to anytype.
    @Field(name = "privateVariable")
    int getPrivateVariableAsInteger();

    @Field(name = "privateVariable")
    void setPrivateVariable(Object someVariable);

    // If return type has @BaseClass annotation.
    // Implementation will be automatically created.
    @Field(name = "privateVariable")
    ExampleWrapper getPrivateVariableAsInteger();

    // @Constructor declares a method to be a constructor
    // The return type MUST be the wrapper interface.
    @Constructor
    ExampleInterface newInstance();
    
    // @Abstracted is used in parameters to indicate 
    // an input being a wrapper interface.
    // The generated code will then unwrap the implementation object.
    void doSomething(@Abstracted ExampleWrapper wrapper);
}
```

To create an implementation of the wrapper interface, use the .create() method in its implementation.
```java
  Object obj = ...;
  ExampleInterface inf = ExampleInterfaceImplementation.create(obj);
```