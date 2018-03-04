package act.db.ebean.util;

/*-
 * #%L
 * ACT Ebean2
 * %%
 * Copyright (C) 2015 - 2018 ActFramework
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

import act.app.App;
import act.asm.AnnotationVisitor;
import act.asm.FieldVisitor;
import act.asm.Type;
import act.db.meta.EntityClassMetaInfo;
import act.db.meta.EntityFieldMetaInfo;
import act.db.meta.EntityMetaInfoRepo;
import act.util.AppByteCodeEnhancer;
import org.osgl.util.S;

// Adapt act timestamp annotation to ebean timestamp annotation
public class TimestampAuditorEnhancer extends AppByteCodeEnhancer<TimestampAuditorEnhancer> {

    private EntityMetaInfoRepo metaInfoRepo;
    private String className;
    private String createdAt;
    private String lastModifiedAt;

    public TimestampAuditorEnhancer() {
        super(S.F.startsWith("act.").negate());
    }

    @Override
    protected Class<TimestampAuditorEnhancer> subClass() {
        return TimestampAuditorEnhancer.class;
    }

    @Override
    public AppByteCodeEnhancer app(App app) {
        metaInfoRepo = app.entityMetaInfoRepo();
        return super.app(app);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        String classDesc = "L" + name + ";";
        className = Type.getType(classDesc).getClassName();
        EntityClassMetaInfo classMetaInfo = metaInfoRepo.classMetaInfo(className);
        if (null != classMetaInfo) {
            EntityFieldMetaInfo fieldMetaInfo = classMetaInfo.createdAtField();
            if (null != fieldMetaInfo) {
                createdAt = fieldMetaInfo.fieldName();
            }
            fieldMetaInfo = classMetaInfo.lastModifiedAtField();
            if (null != fieldMetaInfo) {
                lastModifiedAt = fieldMetaInfo.fieldName();
            }
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        FieldVisitor fv = super.visitField(access, name, desc, signature, value);
        if (null == createdAt && null == lastModifiedAt) {
            return fv;
        }
        final boolean isCreatedAt = name.equals(createdAt);
        final boolean isLastModified = !isCreatedAt && name.equals(lastModifiedAt);
        if (!isCreatedAt && !isLastModified) {
            return fv;
        }
        return new FieldVisitor(ASM5, fv) {
            @Override
            public void visitEnd() {
                AnnotationVisitor av;
                if (isCreatedAt) {
                    av = fv.visitAnnotation("Lcom/avaje/ebean/annotation/WhenCreated;", true);
                } else {
                    av = fv.visitAnnotation("Lcom/avaje/ebean/annotation/WhenModified;", true);
                }
                av.visitEnd();
                super.visitEnd();
            }
        };
    }
}
