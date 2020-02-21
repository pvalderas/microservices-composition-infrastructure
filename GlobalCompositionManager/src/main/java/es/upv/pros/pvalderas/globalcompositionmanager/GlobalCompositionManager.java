package es.upv.pros.pvalderas.globalcompositionmanager;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.ComponentScan;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GlobalCompositionManager {

}
