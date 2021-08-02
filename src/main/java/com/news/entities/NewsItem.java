package com.news.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;

@Getter
@AllArgsConstructor
public class NewsItem {

    private String title, link, description, topic;
    private Date pubDate;

}
