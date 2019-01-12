# Vaadin Flow app with I18N Page Title, dynamically created
What I want to show in this example is, how you could deal with 
a dynamic page title per view (or time or what ever)
that will handle your browser Locale as well.

> All tutorials are available under [https://vaadin.com/tutorials](https://vaadin.com/tutorials)

## recommended to read before
It is recommended to read the tutorial about the I18NProvider first. 
This tutorial will use the *I18NProvider*. 


## setting the Title in Flow
In this solution the title is set inside a constructor of a view.

```java
  public class View001() {
    UI current = UI.getCurrent();
    Locale locale = current.getLocale();
    current
        .getPage()
        .setTitle(Messages.get("global.app.name" , locale) 
                  + " | " 
                  + Messages.get("view001.title" , locale));
  }
````

Here are some points I would like to discuss.
The first is based on the assumption that your application is not only based on one view.
Are you sure you want to write this peace of code a few times? And how often your colleges 
in your team will forget this?
The next is, that here is an implicit definition of a format for the page title.
**xx + " | " + yy** 

We have to extract the repeating part..  This could lead to version 02...

## Solution 02 - A - extracting the formatter

We are extracting the formatting part, first. For this a class with the name **TitleFormatter**
is created. Inside you can define the way, how the page title will be formatted.

```java
public class TitleFormatter {
  public String format(String key, Locale locale){
    return getTranslation("global.app.name" , locale)
           + " | "
           + getTranslation(key , locale);
  }
}
``` 

With this class, 
we have a central place for the definition, how the title should look like.
The next step is, to remove the invocation method out of the constructor.
Vaadin provides an interface called **HasDynamicTitle**. Implementing this, 
you can overwrite the method **getPageTitle()**. But again, you would implement this 
in every view...

```java
@Route(View002.VIEW_002)
public class View002 extends Composite<Div> implements HasDynamicTitle {
  public static final String VIEW_002 = "view002";

  @Override
  public String getPageTitle() {
    UI current = UI.getCurrent();
    Locale locale = current.getLocale();
    return new TitleFormatter().format("view.title", locale);
  }
}
```  

One solution could be based on inheritance, packing this stuff into a parent class.
But how to get the actual **key** to resolve without implementing something in every child class?

## Solution 02 - B - using the annotation
Vaadin will give you one other way to define the page title. The other solution is based on 
an annotation called **PageTitle**

```java
@PageTitle("My PapeTitle")
@Route(View002.VIEW_002)
public class View002 extends Composite<Div> {
  public static final String VIEW_002 = "view002";

}
```

The usage of this Annotation is XOR to the usage of the interface **HasDynamicTitle**.
So, make sure that there is nothing in your inheritance.

The challenge here is based on the fact, that the annotation only consumes static Strings.
I18N is not possible with this solution.

## Solution 03 - my favourite solution ;-)
After playing around with this solutions, I developed a 
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

Now we need a way to resolve the final message and the right point in time to set the title.
Here we could use the following interfaces.

* VaadinServiceInitListener, 
* UIInitListener, 
* BeforeEnterListener

With this interfaces we are able to hook into the life cycle of a view. At this time slots
we have all information's we need. 
The Annotation to get the message key and the locale of the current request is available.

The class that is implementing all these interfaces is called **I18NPageTitleEngine**

```java
public class I18NPageTitleEngine 
       implements VaadinServiceInitListener, 
                  UIInitListener, BeforeEnterListener, HasLogger {


  public static final String ERROR_MSG_NO_LOCALE = "no locale provided and i18nProvider #getProvidedLocales()# list is empty !! ";
  public static final String ERROR_MSG_NO_ANNOTATION = "no annotation found at class ";

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Class<?> navigationTarget = event.getNavigationTarget();
    I18NPageTitle annotation = navigationTarget.getAnnotation(I18NPageTitle.class);
    match(
        matchCase(() -> success(annotation.messageKey())) ,
        matchCase(() -> annotation == null ,
                  () -> failure(ERROR_MSG_NO_ANNOTATION + navigationTarget.getName())) ,
        matchCase(() -> annotation.messageKey().isEmpty() ,
                  () -> success(annotation.defaultValue()))
    )
        .ifPresentOrElse(
            msgKey -> {
              final I18NProvider i18NProvider = VaadinService
                  .getCurrent()
                  .getInstantiator()
                  .getI18NProvider();
              final Locale locale = event.getUI().getLocale();
              final List<Locale> providedLocales = i18NProvider.getProvidedLocales();
              match(
                  matchCase(() -> success(providedLocales.get(0))) ,
                  matchCase(() -> locale == null && providedLocales.isEmpty() ,
                            () -> failure(ERROR_MSG_NO_LOCALE + i18NProvider.getClass().getName())) ,
                  matchCase(() -> locale == null ,
                            () -> success(providedLocales.get(0))) ,
                  matchCase(() -> providedLocales.contains(locale) ,
                            () -> success(locale))
              ).ifPresentOrElse(
                  finalLocale -> ((CheckedFunction<Class<? extends TitleFormatter>, TitleFormatter>) f -> f.getDeclaredConstructor().newInstance())
                      .apply(annotation.formatter())
                      .ifPresentOrElse(
                          formatter -> formatter
                              .apply(i18NProvider , finalLocale , msgKey).
                                  ifPresentOrElse(title -> UI.getCurrent()
                                                             .getPage()
                                                             .setTitle(title) ,
                                                  failed -> logger().info(failed)) ,
                          failed -> logger().info(failed)) ,
                  failed -> logger().info(failed));
            }
            , failed -> logger().info(failed));
  }

  @Override
  public void uiInit(UIInitEvent event) {
    final UI ui = event.getUI();
    ui.addBeforeEnterListener(this);
  }

  @Override
  public void serviceInit(ServiceInitEvent event) {
    event
        .getSource()
        .addUIInitListener(this);
  }
}
```

The method with the name **beforeEnter** is the important part. Here you can see how the key is resolved.
But there is one new thing...  let´s have a look ot the following lines.

```java
              final I18NProvider i18NProvider = VaadinService
                  .getCurrent()
                  .getInstantiator()
                  .getI18NProvider();
```

This few lines are introducing a new thing, that is available in Flow.
The interface **I18NProvider** is used to implement a mechanism for the internationalization 
of Vaadin applications.

To read more about it go to our I18NProvider Tutorial [here ](....)

Last step for today, is the activation of our **I18NPageTitleEngine**
This is done inside the file with the name **com.vaadin.flow.server.VaadinServiceInitListener**
you have to create inside the folder  **META-INF/services** 
The only line we have to add is the full qualified name of our class.

```
com.vaadin.tutorial.flow.i18n.pagetitle.I18NPageTitleEngine
```

If you have questions or something to discuss..  ping me via
email [mailto::sven.ruppert@gmail.com](sven.ruppert@gmail.com)
or via Twitter : [https://twitter.com/SvenRuppert](@SvenRuppert)





