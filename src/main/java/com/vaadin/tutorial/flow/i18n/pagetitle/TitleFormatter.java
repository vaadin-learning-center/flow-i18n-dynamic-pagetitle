package com.vaadin.tutorial.flow.i18n.pagetitle;

import java.util.Locale;

import org.rapidpm.frp.functions.CheckedTriFunction;
import com.vaadin.flow.i18n.I18NProvider;

public interface TitleFormatter extends CheckedTriFunction<I18NProvider, Locale, String, String> {
}
