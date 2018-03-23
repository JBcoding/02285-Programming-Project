find -name "*.java" > sources.txt
javac @sources.txt
java -cp src test.Test -sa
pkill -f 'java -jar'