package vn.stephenphan.memeservice.model;

import lombok.*;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemeTagId implements Serializable {
    private String memeId;
    private String tagId;
}
