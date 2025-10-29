package org.springbozo.meditracker.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public ModelAndView handleError(HttpServletRequest request) {
        Object statusCodeAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object messageAttr = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exceptionAttr = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        int statusCode = 500;
        if (statusCodeAttr instanceof Integer) {
            statusCode = (Integer) statusCodeAttr;
        } else if (statusCodeAttr != null) {
            try {
                statusCode = Integer.parseInt(statusCodeAttr.toString());
            } catch (NumberFormatException ignored) {}
        }

        String errorMsg = null;
        if (exceptionAttr instanceof Exception ex) {
            errorMsg = ex.getMessage();
            if (errorMsg == null && ex.getCause() != null) {
                errorMsg = ex.getCause().toString();
            }
            if (errorMsg == null) {
                errorMsg = ex.toString();
            }
        }
        if (errorMsg == null && messageAttr != null) {
            errorMsg = messageAttr.toString();
        }
        if (errorMsg == null) {
            errorMsg = HttpStatus.valueOf(statusCode).getReasonPhrase();
        }

        ModelAndView mav = new ModelAndView("error");
        mav.addObject("status", statusCode);
        mav.addObject("errorMessage", errorMsg);
        return mav;
    }
}
