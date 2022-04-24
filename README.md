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

There are 2 annotations provided by this API
```java
  @BaseClass  // This Configures the BaseClass that reflection will reflect onto
  @Field      // This signifies a method to be a field accessor
```

You can use the API by creating a wrapper interface. An example interface is given here, it should be self explanatory.
```java
  @BaseClass("com.example.example_project.SomeSecretClass")
  public interface ExampleInterface {

    // If you want to access a method, copy it from the base class.
    void doSomething();

    int getSomeInteger();


    // @Field declares a method to be a field accessor.
    // If return type is void, its a setter or else its a getter.
    @Field(name="privateVariable")
    Object getPrivateVariable();

    // It could cast the returned value to anytype.
    @Field(name="privateVariable")
    int getPrivateVariableAsInteger();

    @Field(name="privateVariable")
    void setPrivateVariable();

    // If retrun type has @BaseClass annotation.
    // Implementation will be automatically created.
    @Field(name="privateVariable")
    ExampleWrapper getPrivateVariableAsInteger();
  }
```

To create a implementation of the wrapper interface, use the .create() method in its implementation.
```java
  Object obj = ...;
  ExampleInterface inf = ExampleInterfaceImplementation.create(obj);
```