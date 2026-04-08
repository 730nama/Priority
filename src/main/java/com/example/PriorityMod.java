package com.example;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.config.HotbarPriorityConfig;

public class PriorityMod implements ModInitializer {
	public static final String MOD_ID = "Priority";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		AutoConfig.register(HotbarPriorityConfig.class, GsonConfigSerializer::new);
		LOGGER.info("Hotbar Priority mod initialized!");
	}
}