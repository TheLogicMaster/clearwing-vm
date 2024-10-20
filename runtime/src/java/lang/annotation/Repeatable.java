package java.lang.annotation;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Repeatable {
    
    Class<? extends Annotation> value();
}
