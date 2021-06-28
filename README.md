## CloudBeat plugin for JUnit5-Java

### Intro
This plugin allows executing Java based JUnit tests using the CloudBeat platform.

### Building
`git clone https://github.com/oxygenhq/cb-framework-plugin-junit`  
`cd cb-framework-plugin-junit`  
`mvn install`  

### Usage
Add the plugin to your project. If you are using a maven based project, you can directly add this library as a dependency:
```xml
<dependency>  
  <groupId>io.cloudbeat.framework</groupId>  
  <artifactId>cb-framework-junit5</artifactId>  
  <version>0.1.0</version>  
</dependency>
```
For running multiple chosen tests install maven surefire plugin with version equal or higher then 2.22.0
```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-surefire-plugin</artifactId>
      <version>2.22.0</version>
    </plugin>
  </plugins>
</build>
``` 

### Working with Selenium

When using Selenium it might be beneficiary to be able to take browser screenshots in case of failures.
This can be achieved in a two different ways. Please note that all 2 options are mutually exclusive.

1. By providing WebDriver instance to the plugin.
2. By providing WebDriver getter method to the plugin.

#### Providing WebDriver instance
```java
public class YourExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource, JUnitRunner {
    @Override
    public void beforeAll(ExtensionContext context) {
            WebDriver driver = ... // WebDriver initialization
            setWebDriver(driver);
    }
}
```

#### Providing WebDriver getter method
```java
public class YourExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource, JUnitRunner {
    @Override
    public void beforeAll(ExtensionContext context) {
       WebDriverProvider provider = new WebDriverProvider();
       setWebDriverGetter(provider::getWebDriver);
    }
}

public class WebDriverProvider {
    public WebDriver getWebDriver() {
       return driver;
    }
}
```

And then in both cases before each class add this code:
```java
@ExtendWith({YourExtension.class})
```
