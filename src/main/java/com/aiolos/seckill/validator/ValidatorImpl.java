package com.aiolos.seckill.validator;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

/**
 * @author Aiolos
 * @date 2019-06-16 14:52
 */
@Component
public class ValidatorImpl implements InitializingBean {

    private Validator validator;

    public ValidationResult validate(Object bean) {

        ValidationResult result = new ValidationResult();
        Set<ConstraintViolation<Object>> cvSet = validator.validate(bean);

        if (cvSet.size() > 0) {

            result.setHasErrors(true);
            cvSet.forEach(constraintViolation -> {
                String propertyName = constraintViolation.getPropertyPath().toString();
                String errMsg = constraintViolation.getMessage();
                result.getErrorMsgMap().put(propertyName, errMsg);

            });
        }

        return result;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }
}
