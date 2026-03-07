package vn.stephenphan.memeservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meme_tags")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(MemeTagId.class)
public class MemeTag {

    @Id
    @Column(name = "meme_id")
    private String memeId;

    @Id
    @Column(name = "tag_id")
    private String tagId;
}
