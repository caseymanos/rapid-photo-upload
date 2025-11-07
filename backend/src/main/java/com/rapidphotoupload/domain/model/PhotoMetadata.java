package com.rapidphotoupload.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PhotoMetadata {
    
    private List<String> tags = new ArrayList<>();
    
    public void addTag(String tag) {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("Tag cannot be empty");
        }
        if (!this.tags.contains(tag.toLowerCase())) {
            this.tags.add(tag.toLowerCase());
        }
    }
    
    public void removeTag(String tag) {
        this.tags.remove(tag.toLowerCase());
    }
    
    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }
}