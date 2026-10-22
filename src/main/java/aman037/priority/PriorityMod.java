package aman037.priority;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PriorityMod implements ModInitializer {
	public static final String MOD_ID = "priority";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// has to happen before anything goes looking for the config holder
		AutoConfig.register(PriorityConfig.class, GsonConfigSerializer::new);
		LOGGER.info("Priority loaded");
	}
}
