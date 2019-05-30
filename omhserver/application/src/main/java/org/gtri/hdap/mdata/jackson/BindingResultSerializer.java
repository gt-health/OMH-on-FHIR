package org.gtri.hdap.mdata.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BeanPropertyBindingResult;

import java.io.IOException;

/**
 * Created by es130 on 8/9/2018.
 */
public class BindingResultSerializer extends StdSerializer<BeanPropertyBindingResult>{

    Logger logger = LoggerFactory.getLogger(BindingResultSerializer.class);

    public BindingResultSerializer(Class<BeanPropertyBindingResult> t) {
        super(t);
    }

    @Override
    public void serialize(BeanPropertyBindingResult bindingResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        logger.debug("Serializing BeanPropertyBindingResult and dropping to the floor.");
    }
}
