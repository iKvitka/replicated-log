FROM hseeberger/scala-sbt:8u222_1.3.5_2.13.1
WORKDIR /www/app

COPY ./ ./

#  # Build it.
RUN sbt compile

EXPOSE 1337
CMD sbt run