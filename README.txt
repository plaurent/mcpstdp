
COMPILING
=========

javac net/pakl/levy/*java


RUNNING
=======

The following command executed a simulation that learns a sequence specified by a settings file called "levy.prop" in the current directory.

java net.pakl.levy.SimulationLearning   

SETTINGS
=========

For the paper "A Neural Network Mechanism for Reward Discounting: Insights from Modeling Hippocampal-Striatal Interactions", the levy.prop settings file is:

n = 4096
a = 0.075
trainingTrials = 120
earlyTrialToSave = 10
connectionProbability = 0.08
isCompetitive = true
synmodrate = 0.01
preserveParameter = 0.8
spacing = 100
patternSize = 100
pExternalOffNoise = 0.3
stutter = 5
sequenceLength = 20


Other example settings files with non-competitive (non-kWTA) networks use include:

// ------------------------------------------------------------------
// EXAMPLE levy.prop 1
// SIMPLE SEQUENCE, 1 PATTERN PER TIMESTEP, OVERLAPPING
// ------------------------------------------------------------------
//n = 1024
//a = 0.10
//Kr = 0.050
//Ki = 0.046
//K0 = 0.0
//w0 = 0.40
//synmodrate = 0.005
//preserveParameter = 0
//spacing = 3
//patternSize = 20
//stutter = 1
//sequenceLength = 20


// ------------------------------------------------------------------
// EXAMPLE levy.prop 2
// STUTTERED PATTERNS with NO OVERLAP
// ------------------------------------------------------------------
//n = 1024
//a = 0.10
//Kr = 0.050
//Ki = 0.046
//K0 = 0.0
//w0 = 0.40
//synmodrate = 0.005
//preserveParameter = 0.5
//spacing = 20
//patternSize = 20
//stutter = 2
//sequenceLength = 10
// ------------------------------------------------------------------



RUNNING A COMPLETE SET OF SIMULATIONS FOR ANALYSIS
==================================================

For the paper, 50 random simulations with the same settings were run.  To do this, place the levy.prop file in a directory called ./SOURCE/ and then, with the compile java files on your CLASSPATH, execute the following perl script.

#!/usr/bin/perl
$alpha = "alpha0.8";
$numSimsInParallel = 10;
for ($i = 1; $i <= 50; $i++)
{
    $j = $i;
    if ($j < 10) { $j = "0".$j; }
    print " mkdir $alpha"."_".$j."\n";
    print " cp SOURCE/levy.prop $alpha"."_".$j."\n";
    print " cd $alpha"."_".$j."\n";
    print "  java net.pakl.levy.SimulationLearning &\n";
    print " cd ..\n";
    if ($i > 0 && $i % $numSimsInParallel == 0) { print "wait\n"; }
}





R CODE FOR GENERATING PLOTS
============================

library("lsa");
library("fields");  # for image.plot

error.bar <- function(x, y, upper, lower=upper, length=0.1,...){
    if(length(x) != length(y) | length(y) !=length(lower) | length(lower) != length(upper))
    stop("vectors must be same length")
    arrows(x,y+upper, x, y-lower, angle=90, code=3,length=length,...)
}

coscomp <- function(M, N) {  
    timesteps <- dim(M)[2];
    result <- matrix(data=NA, nrow=timesteps, ncol=timesteps);
    for (i in 1:timesteps) {
    a <- M[,i];
    alen <- sqrt(a %*% a);
    for (j in 1:timesteps) {
    b <- N[,j];
    blen <- sqrt(b %*% b);
    cosdiff <- (a %*% b) / (alen * blen);
    result[i,j] = cosdiff;
    }
    }
    result
}


S <- 50;     # number of simulations (alpha0.8_01/*, alpha_0.8_02/*, etc)
T <- 100;    # number of time-steps per trial
N <- 4096;   # number of neurons
TSTART = 25; # time-step from which to measure predictive similarity
TEND = 75;   # final time-step

simsum <- c();
simrow <- c();

for (s in 1:S) {
    prefix <- if (s < 10) "0" else "";
    filename <- paste("alpha0.8_", prefix, s, "/finaltrain.txt", sep="")
    print(filename)
    firings <- t(matrix(scan(filename, n=N*T), T, N, byrow=TRUE))
    sim <- cosine(firings);   # uses library "lsa", OR use sim <- coscomp(firings,firings)
    simrow <- rbind(simrow, sim[TSTART, TSTART:TEND])
    simsum <- if (length(simsum) == 0) sim else simsum+sim; 
}
simmean <- simsum / S;
png('similarity.png', width=1000,height=900); par(ps=30);
image.plot(simmean, col=gray(seq(255,1)*1/255), axes=FALSE)
axis(1, axTicks(1), lab=round(seq(0,100,20)))
axis(2, axTicks(2), lab=round(seq(0,100,20)))
box();
dev.off();

simrowmean <- apply(simrow,2,mean)
simrowstd <- apply(simrow,2,sd)
pdf('curve.pdf'); 
par(ps=18);
plot(seq(1,1+TEND-TSTART), simrowmean, type='l', xlab='Time-steps', ylab='Cosine Similarity', ylim=c(0,1), axes=FALSE, lwd=3)
axis(1, axTicks(1), lab=round(seq(TSTART,TEND,10)))
axis(2, seq(0,1,0.25), lab=seq(0,1,0.25))
error.bar(seq(1,1+TEND-TSTART), simrowmean, simrowstd/sqrt(S))
box()
dev.off();


R CODE FOR FITTING (EXPONENTIAL AND HYPERBOLIC)
===============================================

quartz();      # make a new window on OS X
x<-seq(1,51);
y<-simrowmean;

par(new = F);
plot(x, y, xlab='Time-steps', ylab='Cosine Similarity', ylim=c(0,1), axes=FALSE);
par(new = T);
axis(1, axTicks(1), lab=round(seq(25,75,10)))
axis(2, seq(0,1,0.25), lab=seq(0,1,0.25))
box()

fn   <- function(p) sum (( y - (1/(1+p[1]*x)))^2); 
out  <- nlm(fn, p=c(0.4), hessian=TRUE);
yfit <- 1/(1+out$estimate[1]*x);
sse = sum((yfit-y)*(yfit-y))
sst = sum((y-mean(y)) * (y-mean(y)))
RsquaredH = 1-(sse/sst)
RsquaredHADJ = 1 - (1-RsquaredH) * (S-1) / (S-1-1)
sse_hyperbolic <- sqrt(diag(2*out$minimum/(length(y)-2)*solve(out$hessian)));
lines(spline(x, yfit), ylim=c(0,1), col=3, lwd=3);
f_hyperbolic <- paste('1/1+', toString(out$estimate[1]),'x', sep="");

fn   <- function(p) sum (( y - (1/(exp(p[1]*x))))^2); 
out  <- nlm(fn, p=c(0.4), hessian=TRUE);
yfit <- 1/(exp(out$estimate[1]*x));
sse_exponential <- sqrt(diag(2*out$minimum/(length(y)-2)*solve(out$hessian)));
sse = sum((yfit-y)*(yfit-y))
sst = sum((y-mean(y)) * (y-mean(y)))
RsquaredE = 1-(sse/sst)
RsquaredEADJ = 1 - (1-RsquaredE) * (S-1) / (S-1-1)
lines(spline(x, yfit), ylim=c(0,1), col=4, lwd=3);
f_exponential <- paste('1/exp(', toString(out$estimate[1]),'x)', sep="");

legendText <- c(paste('exponential (r^2=', round(RsquaredEADJ, digits=3), ')', sep=""), paste('hyperbolic (r^2=', round(RsquaredHADJ, digits=3),')', sep=""));

legend("topright", legendText, lty=c(1,1), col=c(4,3), lwd=c(3,3))




