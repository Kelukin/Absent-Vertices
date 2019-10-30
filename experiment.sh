#java -cp ./bin/ Main
./build.sh
runJava="java -cp ./bin Main " 
fileDirPath=~/Downloads/datasets/dataFiles/
fileList=$(ls $fileDirPath)

#clear the file
echo " " > record.txt
flag=0
measure_time=0
if [ $# != 0 ]; then
	for z in $@
	do
		if [ $z == "--small" ]; then flag=1
		fi
		if [ $z == "--big" ]; then flag=2
		fi
		if [ $z == "-t" ]; then measure_time=1
		fi
	done
fi
measure_opt=""
if [ $measure_time == 1 ]; then
	measure_opt=" -t "
fi
for i in $fileList
do
	filePath=$fileDirPath$i
	tmp=$(du -b $filePath)
	fileSize=`expr ${tmp%$filePath}`
	if [ $fileSize -gt $((1024*1024*10)) ]  && [ $flag != 1 ]; then
		echo $filePath
		date
		echo $filePath >> record.txt
		$runJava $measure_opt $filePath >> record.txt
	fi
	if [ $(($fileSize)) -le $((1024*1024*10)) ] && [ $flag != 2 ]; then
		echo $filePath >> record.txt
		$runJava $measure_opt $filePath >> record.txt
	fi
done
newFileName=record_new.txt
if [ $flag == 2 ]; then
	newFileName=recordBig.txt
elif [ $flag == 1 ]; then
	newFileName=recordSmall.txt
else
	echo New
fi
mv record.txt $newFileName
