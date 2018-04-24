find -name "*.java" > sources.txt
javac @sources.txt
> MAResults.txt
java -cp src test.Test -ma >> MAResults.txt
pkill -f 'java -jar'
