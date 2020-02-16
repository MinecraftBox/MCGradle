package net.minecraftforge.fml.relauncher;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface SideOnly {
    Side value();
}
