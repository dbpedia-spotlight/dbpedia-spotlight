dirnames=`pwd`/$2
cd $1

cat $dirnames | parallel -j 10 --eta 'if [ -a kba-stream-corpus-2012-cleansed-only/{}/done ] ; then echo "${} was already downloaded"; else wget --recursive --continue --no-host-directories --no-parent --reject "index.html*" http://trec-kba.csail.mit.edu/kba-stream-corpus-2012-cleansed-only/{}/ && for a in `ls kba-stream-corpus-2012-cleansed-only/{}/*gpg`; do gpg $a; rm $a; done; touch kba-stream-corpus-2012-cleansed-only/{}/done; fi'

