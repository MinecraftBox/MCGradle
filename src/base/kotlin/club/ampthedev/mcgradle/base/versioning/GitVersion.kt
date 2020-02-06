package club.ampthedev.mcgradle.base.versioning

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevWalk
import java.io.File

object GitVersion {
    fun gitVersion(dir: File) = try {
        val git = Git.init().setDirectory(dir).call()
        val headId = git.repository.resolve(Constants.HEAD)
        val revs = arrayListOf<String>()
        val repo = git.repository
        RevWalk(repo).use {
            var head = it.parseCommit(headId)
            while (true) {
                revs.add(head.name)
                val parents = head.parents
                if (parents == null || parents.isEmpty()) break
                head = it.parseCommit(parents[0])
            }
        }
        val commitHashToTag = hashMapOf<String, RefWithTagName>()
        val comparator = RefComparator(git)
        for (ref in git.repository.refDatabase.getRefsByPrefix(Constants.R_TAGS)) {
            val withTagName = RefWithTagName(ref, ref.name.substring(Constants.R_TAGS.length))
            val peeled = withTagName.ref.peeledObjectId
            if (peeled == null) {
                updateMap(commitHashToTag, comparator, ref.objectId, withTagName)
            } else {
                updateMap(commitHashToTag, comparator, peeled, withTagName)
            }
        }

        repeat(revs.size) {
            val rev = revs[it]
            if (commitHashToTag.containsKey(rev)) {
                val exactTag = commitHashToTag[rev]!!.tag
                return if (it == 0) {
                    exactTag
                } else {
                    "${revs[0].substring(0, 7)}-SNAPSHOT"
                }
            }
        }
        headId.name.substring(0, 7) + "-SNAPSHOT"
    } catch (e: Exception) {
        "(unknown)"
    }

    private fun updateMap(map: MutableMap<String, RefWithTagName>, comparator: RefComparator, id: ObjectId, ref: RefWithTagName) {
        val commitHash = id.name
        if (map.containsKey(commitHash)) {
            if (comparator.compare(ref, map[commitHash] ?: error("")) < 0) {
                map[commitHash] = ref
            }
        } else {
            map[commitHash] = ref
        }
    }
}