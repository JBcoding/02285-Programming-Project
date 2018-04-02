find -name "*.java" > sources.txt
javac @sources.txt
java -cp src test.Test -ma
pkill -f 'java -jar'
