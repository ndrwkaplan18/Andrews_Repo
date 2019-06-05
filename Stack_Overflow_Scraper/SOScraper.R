scrapeStackoverflowTagPage <- function(url, json = FALSE , max_questions = "all"){
  require(rvest)
  require(jsonlite)
  nextPage <- function(url,current){
    nodes <- read_html(url)
    nextPageNode <- html_node(nodes, paste("a[title$='page ",current+1,"']",sep=""))
    base <- "https://stackoverflow.com"
    nextPageUrl <- paste(base,html_attrs(nextPageNode)[[1]][1],sep="")
    return(nextPageUrl)
  }
  
  getQuestionUrls <- function(url){
    tagPage <- read_html(url)
    nodes <- html_nodes(tagPage,"[class = question-hyperlink]")
    attrs <- html_attrs(nodes)
    href <- c()
    for(x in 1:length(attrs)){
      href <- c(href, attrs[[x]][1])#first item in each element of the list is the url
    }
    noName <- as.vector(href)
    return(grep("^/questions.*$",noName,value=TRUE)) #only want questions, not meta links
  }
  
  getMax_Questions <- function(url,max_questions){
    if(is.character(max_questions)){
      node <- html_node(read_html(url),"div[class$=mb16]")
      node2 <- html_node(node,"div[class$=mr12]")
      text <- html_text(node2)
      return(as.numeric(trimws(gsub("[(\r\n)(questions),]","",text))))
    }
    return(max_questions)
  }
  
  scrapeStackOverflowQuestionPage <- function(url, json = FALSE){ 
    questionPage <- read_html(url)
    #1
    title <- html_text(html_nodes(questionPage,"[class = question-hyperlink]"))[1]
    questionText <- html_text(html_nodes(questionPage,"[class = post-text]")[1])
    qTimeStamp <- gsub("\\r\\n\\s+","",grep("asked",html_text(html_nodes(questionPage,"[class = user-action-time]")),value = TRUE))
    userId <- html_text(html_nodes(questionPage,"[class=d-none]")[1])
    upvoteCount <- html_text(html_nodes(questionPage,"[itemprop=upvoteCount]")[1])
    
    #2
    acceptedAnswerNode <- html_nodes(questionPage, "[itemprop=acceptedAnswer]")
    suggestedAnswerNodes <- html_nodes(questionPage,"[itemprop=suggestedAnswer]")
    
    answerTxt <- html_text(html_nodes(acceptedAnswerNode, "[class = post-text]"))
    answerTxt <- c(answerTxt, html_text(html_nodes(suggestedAnswerNodes, "[class = post-text]")))
    answerTxt <- gsub("\\s+"," ",answerTxt)
    
    answerTime <- html_text(html_nodes(acceptedAnswerNode, "[class = user-action-time]"))
    answerTime <- c(answerTime, html_text(html_nodes(suggestedAnswerNodes, "[class = user-action-time]")))
    answerTime <- grep("answered",answerTime,value = TRUE)
    answerTime <- gsub("\\r\\n\\s+","",answerTime)
    answerTime <- gsub("answered\\s","",answerTime)
    
    acceptedAnswererInfoNodes <- html_nodes(acceptedAnswerNode, "[itemprop = author]")
    suggestedAnswererInfoNodes <- html_nodes(suggestedAnswerNodes, "[itemprop = author]")
    
    answererUId <- html_text(html_node(acceptedAnswererInfoNodes,"a"))
    answererUId <- c(answererUId, html_text(html_nodes(suggestedAnswererInfoNodes, "a")))
    
    answerUpvotes <- html_text(html_node(acceptedAnswerNode, "[itemprop = upvoteCount]"))
    answerUpvotes <- c(answerUpvotes, html_text(html_node(suggestedAnswerNodes, "[itemprop = upvoteCount]")))
    #make list of question info
    question <- list(title, questionText, qTimeStamp, userId, upvoteCount)
    names(question) <- c("Title", "Question Text", "Question Time Stamp", "Asker UID", "Upvote Count")
    #add question list as element in list page_data
    page_data <- list(question)
    names(page_data) <- "Question"
    
    if(length(answerTime) != 0)#only enter the loop if there are answers
      for(x in 1:length(answerTime)){ #use any of the answer variables, as they should all be same length
        #insert list element in page_data containing this iterations data. set names
        page_data[[length(page_data)+1]] <- list(answerTxt[x],answerTime[x],answererUId[x],answerUpvotes[x])
        names(page_data[[x+1]])<- c(paste("Answer", x, "Text"),paste("Answer", x, "Time"),"Answerer UID","Upvote Count")
        names(page_data)[x+1] <- paste("Answer",x)
      }
    
    if(!json){
      return(page_data)
    }
    else{
      return(toJSON(page_data))
    }
  }
  
  max_questions <- getMax_Questions(url,max_questions)
  urls <- getQuestionUrls(url)
  page_data <- list()
  current <- 1
  base <- "https://stackoverflow.com"
  for(x in 1:max_questions){
    page_data[[x]] <- scrapeStackOverflowQuestionPage(paste(base,urls[x],sep = ""))
    Sys.sleep(1) # Be a nice guy!
    names(page_data)[x] <- paste("Question",x)
    #every 15th question, load next page of urls
    if(x %% 15 == 0){
      url <- nextPage(url,current)
      urls <- c(urls,getQuestionUrls(url))
      current <- current + 1
    }
  }#end for loop
  
  if(!json){
    return(page_data)
  }
  else{
    return(toJSON(page_data))
  }
}