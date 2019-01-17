# Vaadin Flow app with I18N Page Title, dynamically created
What I want to show in this example is, how you could deal with 
a dynamic page title per view (or time or what ever)
that will handle your browser Locale as well.

> All tutorials are available under [https://vaadin.com/tutorials](https://vaadin.com/tutorials)

## recommended to read before
It is recommended to read the tutorial about the I18NProvider first. 
This tutorial will use the *I18NProvider*. 




## The Solution 
After playing around a few times, I developed a 
a solution that could handle

* message bundles
* is not inside inheritance
* is based on Annotations
* is easy to extend
* can change the language during runtime

### The developer / user view
Mostly it is a good approach to develop a solution for a developer 
from the perspective of a developer.
Here it means, what should a developer see if he/she have to use your solution.

The developer will see this Annotation.
Three things can be defined here. 

* The message key that will be used to resolve the message based on the actual Locale
* A default value the will be used, if no corresponding resource key was found neither fallback language is provided 
* Definition of the message formatter, default Formatter will only return the translated key.


```java
@Retention(RetentionPolicy.RUNTIME)
public @interface I18NPageTitle {
  String messageKey() default "";
  String defaultValue() default "";
  Class< ? extends TitleFormatter> formatter() default DefaultTitleFormatter.class;
}
```

The default usage should look like the following one.

```java
@Route(View003.VIEW_003)
@I18NPageTitle(messageKey = "view.title")
public class View003 extends Composite<Div> implements HasLogger {
  public static final String VIEW_003 = "view003";
}
```

For more detailed informations about how to implement it, have a look at our tutorial about I18N - Dynamic PageTitle at 
[https://vaadin.com/tutorials](https://vaadin.com/tutorials)

If you have questions or something to discuss..  ping me via
email [mailto::sven.ruppert@gmail.com](sven.ruppert@gmail.com)
or via Twitter : [https://twitter.com/SvenRuppert](@SvenRuppert)





