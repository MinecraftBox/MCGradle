package club.ampthedev.mcgradle.base.versioning

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevWalk
import java.io.IOException

data class RefWithTagName(val ref: Ref, val tag: String)

class RefComparator(git: Git) : Comparator<RefWithTagName> {
    private val walk = RevWalk(git.repository)

    override fun compare(o1: RefWithTagName, o2: RefWithTagName): Int {
        val annotated1 = o1.ref.isAnnotated()
        val annotated2 = o2.ref.isAnnotated()

        if (annotated1 && !annotated2) {
            return -1
        }
        if (!annotated1 && annotated2) {
            return 1
        }

        if (!annotated1) {
            return o1.ref.name.compareTo(o2.ref.name)
        }

        val time1 = o1.ref.getAnnotatedTagDate()
        val time2 = o2.ref.getAnnotatedTagDate()
        if (time1 != null && time2 != null) {
            return time2.compareTo(time1)
        }

        return o1.ref.name.compareTo(o2.ref.name)
    }

    private fun Ref.getAnnotatedTagDate() = try {
        walk.parseTag(objectId).taggerIdent.`when`
    } catch (e: IOException) { null }

    private fun Ref.isAnnotated() = peeledObjectId != null
}