# fmi-avi-messageconverter-tac
fmi-avi-messageconverter module for TAC messages

This project provides conversions between the aviation weather message Java domain model objects defined
in [fmi-avi-messageconverter](https://github.com/fmidev/fmi-avi-messageconverter) and 
the Traditional Alphanumeric Codes (TAC) encoded textual messages.

## Get started
Release artifacts of project are available as maven dependencies in the FMI OS maven repository. To access them, 
add this repository to your project pom, or in your settings:

```xml
<repositories>
  <repository>
    <id>fmi-os-mvn-release-repo</id>
    <url>https://raw.githubusercontent.com/fmidev/fmi-os-mvn-repo/master</url>
    <snapshots>
      <enabled>false</enabled>
    </snapshots>
    <releases>
      <enabled>true</enabled>
      <updatePolicy>daily</updatePolicy>
    </releases>
  </repository>
</repositories>
```

Maven dependency:

```xml
<dependency>
  <groupId>fi.fmi.avi.converter</groupId>
  <artifactId>fmi-avi-messageconverter-tac</artifactId>
  <version>[the release version]</version>
</dependency>
```

The recommended way to using the IWXXM message conversions provided by this project is to inject the conversion 
functionality to the AviMessageParser instance using Spring:

```java
package my.stuff;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.taf.TAF;

@Configuration
@Import(fi.fmi.avi.converter.tac.TACConverter)
public class MyMessageConverterConfig {

    @Autowired
    private AviMessageSpecificConverter<String, METAR> metarTACParser;
    
    @Autowired
    private AviMessageSpecificConverter<String, TAF> tafTACParser;
    
    @Autowired
    private AviMessageSpecificConverter<METAR, String> metarTACSerializer;
    
    @Autowired
    private AviMessageSpecificConverter<TAF, String> tafTACSerializer;
    
    @Bean
    public AviMessageConverter aviMessageConverter() {
        AviMessageConverter p = new AviMessageConverter();
        p.setMessageSpecificConverter(TACConverter.TAC_TO_METAR_POJO, metarTACParser);
        p.setMessageSpecificConverter(TACConverter.TAC_TO_TAF_POJO, tafTACParser);
        p.setMessageSpecificConverter(TACConverter.METAR_POJO_TO_TAC, metarTACSerializer);
        p.setMessageSpecificConverter(TACConverter.TAF_POJO_TO_TAC, tafTACSerializer);
        return p;
    }
}
```

If you want to be able to convert to/from other message encodings (such at IWXXM) too, include the conversion 
modules for them as maven dependencies and add the required converters to the AviMessageConverter configuration.
See [fmi-avi-messageconverter](https://github.com/fmidev/fmi-avi-messageconverter) for more information.


##Supported message conversions

Identifier                                                          | Input                             | Output
--------------------------------------------------------------------|-----------------------------------|-------
fi.fmi.avi.converter.tac.TACConverter.TAC_TO_METAR_POJO | TAC-encoded METAR String | instance of fi.fmi.avi.model.metar.METAR
fi.fmi.avi.converter.tac.TACConverter.TAC_TO_TAF_POJO | TAC-encoded TAF String | instance of fi.fmi.avi.model.taf.TAF
fi.fmi.avi.converter.tac.TACConverter.METAR_POJO_TO_TAC | instance of fi.fmi.avi.model.metar.METAR | TAC-encoded METAR String
fi.fmi.avi.converter.tac.TACConverter.TAF_POJO_TO_TAC | instance of fi.fmi.avi.model.taf.TAF | TAC-encoded TAF String

METAR an TAF are supported but it's expected that the SPECI, SIGMET and AIRMET support will be added as the project becomes more mature.

## Examples

Converting from TAF object to TAC-encoded TAF as String:

```java
TAF pojo = getTAF();
ConversionResult<String> result = converter.convertMessage(pojo, TACConverter.TAF_POJO_TO_TAC);
if (ConversionResult.Status.SUCCESS == result.getStatus()) {
    System.out.println(result.getConvertedMessage());
} else {
    for (ConversionIssue issue:result.getConversionIssues()) {
        System.err.println(issue);
    }
}
```


Converting TAC-encoded METAR to METAR object:

```java
String metarStr = "METAR EFOU 181750Z AUTO 18007KT 9999 OVC010 02/01 Q1015 R/SNOCLO=";
ConversionResult<METAR> result = converter.convertMessage(metarStr, TACConverter.TAC_TO_METAR_POJO);
if (ConversionResult.Status.SUCCESS == result.getStatus()) {
    METAR pojo = result.getConvertedMessage();
    System.out.println(pojo.getAltimeterSettingQNH());
} else {
    for (ConversionIssue issue:result.getConversionIssues()) {
        System.err.println(issue);
    }
}
```


