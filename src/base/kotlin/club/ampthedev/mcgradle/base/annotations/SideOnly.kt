package club.ampthedev.mcgradle.base.annotations

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FIELD)
annotation class SideOnly(val value: Side)