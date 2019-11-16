package tk.amplifiable.mcgradle

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet

class MCGradleGroovy {
    static void addSourceSets(Project toAddTo, SourceSet set, String ktVer) {
        if (ktVer != null) {
            set.kotlin {
                srcDirs = [new File(toAddTo.getProjectDir(), 'src/main/java').getAbsolutePath(), new File(toAddTo.getRootDir(), 'src/main/kotlin').getAbsolutePath(), new File(toAddTo.getRootDir(), 'src/main/java').getAbsolutePath()]
            }
            set.java {
                srcDirs = [new File(toAddTo.getProjectDir(), 'src/main/java').getAbsolutePath(), new File(toAddTo.getRootDir(), 'src/main/java').getAbsolutePath()]
            }
        } else {
            set.java {
                srcDirs = [new File(toAddTo.getProjectDir(), 'src/main/java').getAbsolutePath(), new File(toAddTo.getRootDir(), 'src/main/java').getAbsolutePath()]
            }
        }
        set.resources {
            srcDirs = [new File(toAddTo.getProjectDir(), 'src/main/resources').getAbsolutePath(), new File(toAddTo.getRootDir(), 'src/main/resources').getAbsolutePath()]
        }
    }
}
