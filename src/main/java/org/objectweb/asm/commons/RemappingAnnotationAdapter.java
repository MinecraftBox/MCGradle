package org.objectweb.asm.commons;

import org.objectweb.asm.AnnotationVisitor;

public class RemappingAnnotationAdapter extends AnnotationRemapper {
    public RemappingAnnotationAdapter(AnnotationVisitor annotationVisitor, Remapper remapper) {
        super(annotationVisitor, remapper);
    }

    public RemappingAnnotationAdapter(int api, AnnotationVisitor annotationVisitor, Remapper remapper) {
        super(api, annotationVisitor, remapper);
    }
}
