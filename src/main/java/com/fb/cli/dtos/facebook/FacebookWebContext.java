package com.fb.cli.dtos.facebook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacebookWebContext implements Serializable {
    private String userId;
    private String jazoest;
    private String lsdToken;
    private String dtsgToken;
}
