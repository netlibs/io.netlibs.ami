package io.netlibs.ami.pump.utils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

public class ObjectMapperFactory {

	private static final ObjectMapper mapper = new ObjectMapper()
			.registerModules(new JavaTimeModule(), new ParameterNamesModule(), new Jdk8Module(), new GuavaModule())
			.setDefaultPropertyInclusion(Include.NON_DEFAULT);

	public static ObjectMapper objectMapper() {
		return mapper;
	}

}