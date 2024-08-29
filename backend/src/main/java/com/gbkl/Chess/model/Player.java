package com.gbkl.Chess.model;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Player {
    String uid;
    String name;
    String Color;
}
