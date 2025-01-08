package java.lang.reflect;

import java.lang.annotation.Annotation;

public class AccessibleObject implements AnnotatedElement {

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> aClass) {
        return null;
    }

    @Override
    public Annotation[] getAnnotations() {
        return new Annotation[0];
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return new Annotation[0];
    }

    public static void setAccessible(AccessibleObject[] array, boolean flag) {
    }

    public void setAccessible(boolean flag) {
    }

    public final boolean trySetAccessible() {
        return true;
    }

    public boolean isAccessible() {
        return true;
    }

    public final boolean canAccess(Object obj) {
        return true;
    }
}
