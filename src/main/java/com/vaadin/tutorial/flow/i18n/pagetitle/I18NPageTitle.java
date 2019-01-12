package com.vaadin.tutorial.flow.i18n.pagetitle;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface I18NPageTitle {
  String messageKey() default "";
  String defaultValue() default "";
  Class< ? extends TitleFormatter> formatter() default DefaultTitleFormatter.class;
}
