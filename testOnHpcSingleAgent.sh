find -name "*.java" > sources.txt
javac @sources.txt
> SAResults.txt
java -cp src test.Test -sa >> SAResults.txt
pkill -f 'java -jar'
