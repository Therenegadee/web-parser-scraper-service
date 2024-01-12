package parser.app.webscraper.models;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StorageItem {
    private String name;
    private List<String> tags;
}