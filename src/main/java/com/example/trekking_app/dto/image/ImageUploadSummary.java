package com.example.trekking_app.dto.image;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageUploadSummary {

 private List<String> uploaded;
    private List<String> skipped;
   private  List<String> failed;
}
