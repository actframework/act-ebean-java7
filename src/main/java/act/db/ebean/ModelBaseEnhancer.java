package act.db.ebean;

import act.asm.AnnotationVisitor;
import act.asm.Type;
import act.util.AppByteCodeEnhancer;
import org.osgl.$;

import javax.persistence.MappedSuperclass;

/**
 * Add {@code javax.persistence.MappedSuperclass} annotation to {@link act.db.ModelBase}
 */
public class ModelBaseEnhancer extends AppByteCodeEnhancer<ModelBaseEnhancer> {

    private boolean shouldEnhance;

    public ModelBaseEnhancer() {
        super($.F.<String>yes());
    }

    @Override
    protected Class<ModelBaseEnhancer> subClass() {
        return ModelBaseEnhancer.class;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        String className = name.replace('/', '.').replace('$', '.');
        if (className.equals("act.db.ModelBase")) {
            shouldEnhance = true;
        }
    }

    @Override
    public void visitEnd() {
        if (shouldEnhance) {
            addAnnotation();
        }
        super.visitEnd();
    }

    private void addAnnotation() {
        AnnotationVisitor av = visitAnnotation(Type.getType(MappedSuperclass.class).getDescriptor(), true);
        av.visitEnd();
    }
}
