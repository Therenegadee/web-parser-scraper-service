package parser.app.webscraper.models;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import parser.app.webscraper.models.enums.ElementType;
import parser.app.webscraper.scraperlogic.logic.element.CssSelectorElement;
import parser.app.webscraper.scraperlogic.logic.element.TagAttrElement;
import parser.app.webscraper.scraperlogic.logic.element.XPathElement;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = XPathElement.class, name = "XPath"),
        @JsonSubTypes.Type(value = CssSelectorElement.class, name = "CSS Selector"),
        @JsonSubTypes.Type(value = TagAttrElement.class, name = "Tag+Attribute"),
})
public class ElementLocator {
    private Long id;
    private String name;
    private ElementType type;
    private String pathToLocator;
    private String extraPointer; // for Tag + Attribute
    private UserParserSetting userParserSetting;
}
