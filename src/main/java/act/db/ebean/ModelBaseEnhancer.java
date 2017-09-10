package act.db.ebean;

/*-
 * #%L
 * ACT Ebean
 * %%
 * Copyright (C) 2015 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
