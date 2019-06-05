*****************************************************************************************************************************************
                                                      STACK OVERFLOW TOPIC PAGE SCRAPER
*****************************************************************************************************************************************

General function for collecting information in ALL StackOverflow.com question pages on a given topic
ex) https://stackoverflow.com/questions/tagged/r?sort=frequent&pageSize=15

function takes arguments url, json, max_questions

url should be a StackOverflow tag page
json is a boolean value. TRUE - information stored in json format. FALSE - informations stored in R list. Default FALSE
max_questions - maximum question pages to scrape. Default is "all" (NOT recommended, could be a ton of pages)

Script intentionally waits 1 second between each page as to not overload StackOverflow servers. We love StackOverflow and do not want
to hurt it.
