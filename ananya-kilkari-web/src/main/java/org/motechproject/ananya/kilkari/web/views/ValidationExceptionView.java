package org.motechproject.ananya.kilkari.web.views;

import org.motechproject.ananya.kilkari.exceptions.ValidationException;
import org.motechproject.ananya.kilkari.web.contract.response.BaseResponse;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class ValidationExceptionView extends KilkariView {
    @Override
    protected void renderMergedOutputModel(
            Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

        ValidationException exceptionObject =
                (ValidationException) model.get(SimpleMappingExceptionResolver.DEFAULT_EXCEPTION_ATTRIBUTE);

        response.getOutputStream().print(BaseResponse.failure(exceptionObject.getMessage()).toJson());

        setHttpStatusCodeBasedOnChannel(request, response);
        setContentTypeToJavaScriptForIVRChannel(request, response);
    }
}
