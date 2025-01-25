FROM gradle:jdk21-corretto
WORKDIR /app
COPY . .
CMD ["gradle", "jmh", "--no-configuration-cache"]

# Build the image
# Example: docker build -t fuzzier-perf .
# Example: docker run --memory="3G" --cpus="2.0" -d fuzzier-perf
