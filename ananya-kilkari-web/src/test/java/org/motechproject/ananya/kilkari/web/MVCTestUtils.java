package org.motechproject.ananya.kilkari.web;

import org.junit.Ignore;
import org.motechproject.ananya.kilkari.web.interceptors.KilkariChannelInterceptor;
import org.springframework.test.web.server.MockMvc;
import org.springframework.test.web.server.setup.MockMvcBuilders;
import org.springframework.test.web.server.setup.StandaloneMockMvcBuilder;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.Arrays;

@Ignore
public class MVCTestUtils {

    public static MockMvc mockMvc(Object controller) {
        StandaloneMockMvcBuilder mockMvcBuilder = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(new KilkariChannelInterceptor())
                .setViewResolvers(new KilkariViewResolver());

        mockMvcBuilder.setHandlerExceptionResolvers(Arrays.asList(new HandlerExceptionResolver[]{new KilkariExceptionResolver()}));
        return mockMvcBuilder.build();
    }
}
