FROM hseeberger/scala-sbt:8u222_1.3.5_2.13.1 as devopd_cousrse
WORKDIR /www/app

COPY ./ ./

#  # Build it.
RUN sbt compile

FROM hseeberger/scala-sbt:8u222_1.3.5_2.13.1
WORKDIR /replicated-log

COPY --from=devopd_cousrse /home /home
COPY --from=devopd_cousrse /www/app/build.sbt ./
COPY --from=devopd_cousrse /www/app/project ./project/
COPY --from=devopd_cousrse /www/app/src ./src/

CMD sbt "runMain master.Master"

#The problem:
#    base docker image `hseeberger/scala-sbt:8u222_1.3.5_2.13.1` have size of 721MB

#    If we build with only one stage we will get image with size of 1.74GB
#    replicated-log latest  83dfb48a6cec    4 months ago    1.7GB
#    Is it Jar deppendensis that take 1GB? XD

#    easy fix is use multistage build and copy only needed files

#    In result I got image with size of 1.1GB
#    dev_ops_course latest  a4320dc6bd69    20 seconds ago  1.1GB