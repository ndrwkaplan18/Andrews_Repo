setwd("C:/YeshivaUniversity/ClassesSpring2019/Bus.Intel.And.Consumer.Insight/Homework/Project")
dataset <- read.csv("hmeq.csv")
training <- read.csv("training.csv")
testing <- read.csv("testing.csv")
df <- na.omit(dataset)
df <- df[-nrow(df),] #even out number of observations

#Check for correlations
corrmat <- cor(df[,-c(5,6)])
library(lattice)
levelplot(corrmat,scales=list(x=list(rot=90)))

#Amount due on existing property and value of current property are highly correlated
#make pca of them
mortDue_cent <- df$MORTDUE - mean(df$MORTDUE)
value_cent <- df$VALUE - mean(df$VALUE)
data <- data.frame(mortDue_cent,value_cent)
pca=prcomp(data)
screeplot(pca,type="lines")
#only use pca1
drops <- c("VALUE","MORTDUE")
MORTDUE <- df$MORTDUE
VALUE <- df$VALUE
df <- df[,!(names(df) %in% drops)]
df <- cbind(df,pca$x[,1],MORTDUE,VALUE)

rm(corrmat,data,pca,drops,mortDue_cent,value_cent,dataset)
#break into test and training
rand <- sample(1:3514,replace = FALSE)
training <- df[rand[351:length(rand)],]
testing <- df[rand[1:350],]

#Logistic

#Backwards Selection (Logit)
model=glm(BAD~.,family=binomial(),data=training)
prob= predict(model, newdata=testing, type="response")
pred= (prob>.5)*1
missclass=sum(testing$BAD!=pred)/nrow(testing)
missclass 

for(i in 2:ncol(training)){
  model=glm(BAD~.,family=binomial(),data=training[,-i])
  prob= predict(model, newdata=testing[,-i], type="response")
  pred= (prob>.5)*1
  missclass=c(missclass,sum(testing$BAD!=pred)/nrow(testing))
}
which(missclass == max(missclass))
omit <- which(missclass == max(missclass))
which(missclass == min(missclass))
prevMin <- min(missclass)
training2 <- training[,-omit]
testing2 <- testing[,-omit]
missclass <- prevMin
rm(prevMin)
for(i in 2:ncol(training2)){
  model=glm(BAD~.,family=binomial(),data=training2[,-i])
  prob= predict(model, newdata=testing2[,-i], type="response")
  pred= (prob>.5)*1
  missclass=c(missclass,sum(testing2$BAD!=pred)/nrow(testing2))
}
which(missclass == max(missclass))
which(missclass == min(missclass))
missclassLogitBackwards <- min(missclass)
#stop
#use every col except 11

#Backwards selection(Probit)
model=glm(BAD~.,family=binomial("probit"),data=training)
prob= predict(model, newdata=testing, type="response")
pred= (prob>.5)*1
missclass=sum(testing$BAD!=pred)/nrow(testing)
missclass

for(i in 2:ncol(training)){
  model=glm(BAD~.,family=binomial("probit"),data=training[,-i])
  prob= predict(model, newdata=testing[,-i], type="response")
  pred= (prob>.5)*1
  missclass=c(missclass,sum(testing$BAD!=pred)/nrow(testing))
}
which(missclass == max(missclass))
omit <- which(missclass == max(missclass))
which(missclass == min(missclass))
prevMin <- min(missclass)
training2 <- training[,-omit]
testing2 <- testing[,-omit]
missclass <- prevMin
rm(prevMin)
for(i in 2:ncol(training2)){
  model=glm(BAD~.,family=binomial("probit"),data=training2[,-i])
  prob= predict(model, newdata=testing2[,-i], type="response")
  pred= (prob>.5)*1
  missclass=c(missclass,sum(testing2$BAD!=pred)/nrow(testing2))
}
which(missclass == max(missclass))
which(missclass == min(missclass))
#stop
missclassProbitBackwards <- min(missclass)

#forwards selection (Logit)
missclass <- 1
for(i in 2:ncol(training)){
  model=glm(BAD~.,family=binomial(),data=training[,c(1,i)])
  prob= predict(model, newdata=testing[,c(1,i)], type="response")
  pred= (prob>.5)*1
  missclass=c(missclass,sum(testing$BAD!=pred)/nrow(testing))
}
which(missclass == min(missclass))
include <- which(missclass == min(missclass)) #include 11
missclass <- min(missclass)
for(i in 2:ncol(training)){
  if(i %in% include){next}
  model=glm(BAD~.,family=binomial(),data=training[,c(1,include,i)])
  prob= predict(model, newdata=testing[,c(1,include,i)], type="response")
  pred= (prob>.5)*1
  missclass=c(missclass,sum(testing$BAD!=pred)/nrow(testing))
}
which(missclass == min(missclass))
include <- c(include,which(missclass == min(missclass))) #include 11 and 6
missclass <- min(missclass)
for(i in 2:ncol(training)){
  if(i %in% include){next}
  model=glm(BAD~.,family=binomial(),data=training[,c(1,include,i)])
  prob= predict(model, newdata=testing[,c(1,include,i)], type="response")
  pred= (prob>.5)*1
  missclass=c(missclass,sum(testing$BAD!=pred)/nrow(testing))
}
which(missclass == min(missclass))
include <- c(include, which(missclass == min(missclass))) #include 2, 6, 11
missclass <- min(missclass) 
for(i in 2:ncol(training)){
  if(i %in% include){next}
  model=glm(BAD~.,family=binomial(),data=training[,c(1,include,i)])
  prob= predict(model, newdata=testing[,c(1,include,i)], type="response")
  pred= (prob>.5)*1
  missclass=c(missclass,sum(testing$BAD!=pred)/nrow(testing))
}
which(missclass == min(missclass))
include <- c(include, which(missclass == min(missclass))+2) #include 2, 6, 8, 11
missclass <- min(missclass) 
for(i in 2:ncol(training)){
  if(i %in% include){next}
  model=glm(BAD~.,family=binomial(),data=training[,c(1,include,i)])
  prob= predict(model, newdata=testing[,c(1,include,i)], type="response")
  pred= (prob>.5)*1
  missclass=c(missclass,sum(testing$BAD!=pred)/nrow(testing))
}
which(missclass == min(missclass))
missclassLogitForwards = min(missclass)
#stop
#include cols 2, 6, 8, 11

#forwards selection (Probit)
missclass <- 1
for(i in 2:ncol(training)){
  model=glm(BAD~.,family=binomial("probit"),data=training[,c(1,i)])
  prob= predict(model, newdata=testing[,c(1,i)], type="response")
  pred= (prob>.5)*1
  missclass=c(missclass,sum(testing$BAD!=pred)/nrow(testing))
}
which(missclass == min(missclass))
include <- which(missclass == min(missclass)) #include 6
missclass <- min(missclass)
for(i in 2:ncol(training)){
  if(i %in% include){next}
  model=glm(BAD~.,family=binomial("probit"),data=training[,c(1,include,i)])
  prob= predict(model, newdata=testing[,c(1,include,i)], type="response")
  pred= (prob>.5)*1
  missclass=c(missclass,sum(testing$BAD!=pred)/nrow(testing))
}
which(missclass == min(missclass))
include <- c(include,which(missclass == min(missclass))[1]+1) #include 9 and 6
#Two mins. Skip to line 191 to process with 10 instead
missclass <- min(missclass)
for(i in 2:ncol(training)){
  if(i %in% include){next}
  model=glm(BAD~.,family=binomial("probit"),data=training[,c(1,include,i)])
  prob= predict(model, newdata=testing[,c(1,include,i)], type="response")
  pred= (prob>.5)*1
  missclass=c(missclass,sum(testing$BAD!=pred)/nrow(testing))
}
which(missclass == min(missclass))
include <- c(include, which(missclass == min(missclass))+2) #include 6, 9, 11
missclass <- min(missclass)
for(i in 2:ncol(training)){
  if(i %in% include){next}
  model=glm(BAD~.,family=binomial("probit"),data=training[,c(1,include,i)])
  prob= predict(model, newdata=testing[,c(1,include,i)], type="response")
  pred= (prob>.5)*1
  missclass=c(missclass,sum(testing$BAD!=pred)/nrow(testing))
}
which(missclass == min(missclass))
mins <- min(missclass)

include <- c(include,which(missclass == min(missclass))[2]+1) #include 11 and 6
missclass <- min(missclass)
for(i in 2:ncol(training)){
  if(i %in% include){next}
  model=glm(BAD~.,family=binomial("probit"),data=training[,c(1,include,i)])
  prob= predict(model, newdata=testing[,c(1,include,i)], type="response")
  pred= (prob>.5)*1
  missclass=c(missclass,sum(testing$BAD!=pred)/nrow(testing))
}
which(missclass == min(missclass))
mins <- c(mins,min(missclass))
mins # they're the same
missclassProbitForwards = min(missclass)
#stop
#include cols 6,8,11

missclassLogitBackwards 
missclassProbitBackwards 
missclassLogitForwards 
missclassProbitForwards 
#missclassLogitForwards wins
logisticModel = glm(BAD~.,family=binomial(),data=training[,c(1,2,6,8,11)])
prob= predict(logisticModel, newdata=testing, type="response")
pred= (prob>.5)*1
logisticMissclass=sum(testing$BAD!=pred)/nrow(testing)

rm(model,testing2,training2,i,include,mins,missclassLogitBackwards,missclassLogitForwards,missclassProbitBackwards,missclassProbitForwards,omit,pred,prob)


#Re-adding MORTDUE and VALUE to the dataset
training <- cbind(training,MORTDUE[rand[351:length(rand)]],VALUE[rand[351:length(rand)]])
testing <- cbind(testing,MORTDUE[rand[1:350]],VALUE[rand[1:350]])
colnames(training)[12:14] <- c("PCA","MORTDUE","VALUE")
colnames(testing)[12:14] <- c("PCA","MORTDUE","VALUE")
training <- training[,!(names(training) %in% "PCA")]
testing <- testing[,!(names(testing) %in% "PCA")]
training <- training[,-c(14:15)]
testing <- testing[,-c(14:15)]
#Decision Tree

library(rpart)
library(rpart.plot)
#Make a decision tree considering all possible variables with default cp = 0.008
tree = rpart(BAD~.,data = training)
pred= predict(tree,newdata=testing, type="vector") > 0.5
missclass = sum(testing$BAD!=pred)/nrow(testing)
missclass
pCp <- printcp(tree,digits=5)
bestCp <- as.vector(pCp[,1][which(pCp[,4] == min(pCp[,4]))])
#xerror is minimized right before cp = 0.01
tree = rpart(BAD~.,data = training, control=rpart.control(cp=bestCp))
pred= predict(tree,newdata=testing, type="vector") > 0.5
missclass = c(missclass,sum(testing$BAD!=pred)/nrow(testing))
missclass

#bestCp gave a same missclass!
tree = rpart(BAD~.,data = training, control=rpart.control(cp=0.001))
pred= predict(tree,newdata=testing, type="vector") > 0.5
missclass = c(missclass,sum(testing$BAD!=pred)/nrow(testing))
missclass

#Lower cp gave better missclass!
#Try something higher
tree = rpart(BAD~.,data = training, control=rpart.control(cp=0.05))
pred= predict(tree,newdata=testing, type="vector") > 0.5
missclass = c(missclass,sum(testing$BAD!=pred)/nrow(testing))
missclass

#missclass seems to have bottomed out with cp = 0.001
dtModel = rpart(BAD~.,data = training, control=rpart.control(cp=0.001))
pred= predict(dtModel,newdata=testing, type="vector") > 0.5
dtMissclass = sum(testing$BAD!=pred)/nrow(testing)
rm(tree,bestCp,pCp,pred,missclass,MORTDUE,VALUE)
rpart.plot(dtModel,digits=-1,tweak = 2.5,uniform=TRUE)

#SVM
library(e1071)

#First try linear low cost
svmModel=svm(BAD~.,data=training,kernel="linear",scale=TRUE,type="C-classification",cost=1)
pred = predict(svmModel,newdata = testing)
missclass = sum(testing$BAD!=pred)/nrow(testing)

#Now radial low cost
svmModel=svm(BAD~.,data=training,kernel="radial",scale=TRUE,type="C-classification",cost=1)
pred = predict(svmModel,newdata = testing)
missclass = c(missclass, sum(testing$BAD!=pred)/nrow(testing))
missclass

#radial performed better, try linear higher cost
svmModel=svm(BAD~.,data=training,kernel="linear",scale=TRUE,type="C-classification",cost=100)
pred = predict(svmModel,newdata = testing)
missclass = c(missclass, sum(testing$BAD!=pred)/nrow(testing))
missclass

#linear higher cost is worse. Try radial higher cost
svmModel=svm(BAD~.,data=training,kernel="radial",scale=TRUE,type="C-classification",cost=100)
pred = predict(svmModel,newdata = testing)
missclass = c(missclass, sum(testing$BAD!=pred)/nrow(testing))
missclass

#That's crazy low!
#Try again even higher cost
svmModel=svm(BAD~.,data=training,kernel="radial",scale=TRUE,type="C-classification",cost=1000)
pred = predict(svmModel,newdata = testing)
missclass = c(missclass, sum(testing$BAD!=pred)/nrow(testing))
missclass

#no change
#go with radial cost=100
svmModel = svmModel=svm(BAD~.,data=training,kernel="radial",scale=TRUE,type="C-classification",cost=100)
pred = predict(svmModel,newdata = testing)
svmMissclass = sum(testing$BAD!=pred)/nrow(testing)

rm(pred,missclass)

#KNN
library(class)
library(FastKNN)

#Remove qualitative columns for knn
drops <- c("REASON","JOB")
knnTesting <- testing[,!(names(testing) %in% drops)]
knnTraining <- training[,!(names(training) %in% drops)]
rm(drops)

#First pass
pred = knn(train = knnTraining[,-1], test = knnTesting[,-1], cl = knnTraining[,1], k = 50)
missclass=sum(knnTesting$BAD!=pred)/nrow(knnTesting)
missclass

#Bring everything into the same scale (excluding BAD)
for(i in 2:(ncol(knnTesting))){
  knnTesting[,i]=(knnTesting[,i]-mean(knnTesting[,i]))/sd(knnTesting[,i])
}
for(i in 2:(ncol(knnTraining))){
  knnTraining[,i]=(knnTraining[,i]-mean(knnTraining[,i]))/sd(knnTraining[,i])
}

#now try
pred = knn(train = knnTraining[,-1], test = knnTesting[,-1], cl = knnTraining[,1], k = 50)
missclass=c(missclass, sum(knnTesting$BAD!=pred)/nrow(knnTesting))
missclass
#bringing to same scale brought down missclass, but still doesn't look too good

#try a bunch of them and see where k is best
missclass=c()
for(k in 1:200){ ## Note that in the interest of time we will consider k=4,8,12,16, etc.
  pred = knn(train = knnTraining[,-1], test = knnTesting[,-1], cl = knnTraining[,1], k = k)
  missclass=c(missclass, sum(knnTesting$BAD!=pred)/nrow(knnTesting))
}
plot(1:200, missclass,pch=16)

#choose k = 3
pred = knn(train = knnTraining[,-1], test = knnTesting[,-1], cl = knnTraining[,1], k = 3)
knnMissclass= sum(knnTesting$BAD!=pred)/nrow(knnTesting)
knnMissclass

rm(k,pred,missclass,i)

#Cluster
clusterData = rbind(knnTraining,knnTesting)
euclidianDist = dist(clusterData[,-1])
manhattanDist = dist(clusterData[,-1], method = "manhattan")
chessDist = dist(clusterData[,-1], method = "maximum")

euclidianClusters = hclust(euclidianDist,method = "complete")
euclidianClusters2 = hclust(euclidianDist, method = "average")
manhattanClusters = hclust(manhattanDist,method = "complete")
chessClusters = hclust(chessDist,method = "complete")

plot(as.dendrogram(euclidianClusters))
plot(as.dendrogram(euclidianClusters2))
plot(as.dendrogram(manhattanClusters))
plot(as.dendrogram(chessClusters))

euclidianGroups = cutree(euclidianClusters,k=2)
euclidianGroups2 = cutree(euclidianClusters2,k=2)
manhattanGroups = cutree(manhattanClusters,k=2)
chessGroups = cutree(chessClusters,k=2)


sum(clusterData$BAD[euclidianGroups==1]==1)
sum(clusterData$BAD[euclidianGroups==1]==0)
#Group 1 should be 0

sum(clusterData$BAD[euclidianGroups2==1]==1)
sum(clusterData$BAD[euclidianGroups2==1]==0)
#Group 1 should be 0

sum(clusterData$BAD[manhattanGroups==1]==1)
sum(clusterData$BAD[manhattanGroups==1]==0)
293/sum(df$BAD == 1)
3129/sum(df$BAD == 0)
#Group 1 should be 0

sum(clusterData$BAD[chessGroups==1]==1)
sum(clusterData$BAD[chessGroups==1]==0)
#Group 1 should be 0

euclidPred = rep(0,3514)
euclidPred2 = rep(0,3514)
manhatPred = rep(0,3514)
chessPred = rep(0,3514)

euclidPred[euclidianGroups == 2] = 1
euclidPred2[euclidianGroups2 == 2] = 1
manhatPred[manhattanGroups == 2] = 1
chessPred[chessGroups == 2] = 1

euclidMissclass=sum(df$BAD!=euclidPred)/nrow(df)
euclidMissclass2=sum(df$BAD!=euclidPred2)/nrow(df)
manhatMissclass=sum(df$BAD!=manhatPred)/nrow(df)
chessMissclass=sum(df$BAD!=chessPred)/nrow(df)

clusterMissclass = c(euclidMissclass,euclidMissclass2,manhatMissclass,chessMissclass)
which(clusterMissclass == min(clusterMissclass))
clusterMissclass = chessMissclass
clusterModel = chessGroups

rm(manhattanDist,chessDist,euclidianClusters,euclidianClusters2,manhatMissclass,chessMissclass,euclidianDist,
   euclidianGroups,euclidianGroups2,manhattanClusters,chessClusters,manhattanGroups,
   chessGroups,manhatPred,chessPred,euclidPred,euclidPred2,euclidMissclass,euclidMissclass2)

#Random Forest
library(randomForest)
forestTraining = training
forestTraining$BAD = as.factor(forestTraining$BAD)
forestTesting = testing
forestTesting$BAD = as.factor(forestTesting$BAD)

rf1 = randomForest(BAD~.,data = forestTraining)
pred = predict(rf1, newdata = forestTesting)
rfMissclass = sum(testing$BAD!=pred)/nrow(testing)

rf2 = randomForest(BAD~.,data = forestTraining, ntree = 100)
pred = predict(rf2, newdata = forestTesting)
rfMissclass = c(rfMissclass,sum(testing$BAD!=pred)/nrow(testing))

rf3 = randomForest(BAD~.,data = forestTraining, ntree = 1000)
pred = predict(rf3, newdata = forestTesting)
rfMissclass = c(rfMissclass,sum(testing$BAD!=pred)/nrow(testing))

rf4 = randomForest(BAD~.,data = forestTraining, ntree = 500, mtry = 5)
pred = predict(rf4, newdata = forestTesting)
rfMissclass = c(rfMissclass,sum(testing$BAD!=pred)/nrow(testing))
rfMissclass
which(rfMissclass == min(rfMissclass))
min(rfMissclass)
#3 wins
rfMissclass = rfMissclass[3]
rfModel = rf3

rm(rf1,rf2,rf3,rf4,pred)

#Final check
#Logistic
pred= (predict(logisticModel, newdata=testing, type="response") > .5) * 1
allMissclass = sum(testing$BAD!=pred)/nrow(testing)
#Decision Tree
pred= predict(dtModel,newdata=testing, type="vector") > 0.5
allMissclass = c(allMissclass,sum(testing$BAD!=pred)/nrow(testing))
#SVM
pred = predict(svmModel,newdata = testing)
allMissclass = c(allMissclass, sum(testing$BAD!=pred)/nrow(testing))
#KNN
pred = knn(train = knnTraining[,-1], test = knnTesting[,-1], cl = knnTraining[,1], k = 3)
allMissclass = c(allMissclass,sum(knnTesting$BAD!=pred)/nrow(knnTesting))
#Cluster
chessDist = dist(clusterData[,-1], method = "maximum")
Clusters = hclust(chessDist,method = "complete")
Groups = cutree(Clusters,k=2)
Pred = rep(0,3514)
Pred[Groups == 2] = 1
allMissclass = c(allMissclass,sum(df$BAD!=Pred)/nrow(df))
#Random Forest
pred = predict(rfModel, newdata = forestTesting)
allMissclass = c(allMissclass,sum(testing$BAD!=pred)/nrow(testing))
allMissclass
which(allMissclass == min(allMissclass))
#SVM wins!