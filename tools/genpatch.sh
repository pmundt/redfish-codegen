#!/bin/bash -xe
# Generates some patches in the set that fixes the OpenAPI documents for code
# generation.

SCRIPT_DIR=$(dirname $0)

while getopts "d:" OPT; do
	case "${OPT}" in
	d) BUILD_DIRECTORY=$OPTARG ;;
	esac
done

export QUILT_PC=${BUILD_DIRECTORY}/.pc
export QUILT_PATCHES=schema-patches

PATCHES=(
	"0001-Translate-openapi-fragments.patch"
)

SCRIPTS=(
	"${SCRIPT_DIR}/fix_openapi_fragments.py"
)

if [ ! -d schema-patches ]; then
	printf >&2 "This script should be run from the root of the repository.\n"
	exit 1
fi

update_patches=true
schema_hashfile="schema-patches/.hash"

setup() {
	make -f generate.mk clean && make -f generate.mk unzip

	# Get the current hash
	current_hash=$(md5sum ${BUILD_DIRECTORY}/openapi/*.yaml | md5sum | awk '{ print $1 }')

	# Compare against the hash the patches were previously generated against, if it exists
	if [ -f "$schema_hashfile" ]; then
		last_hash=$(cat ${schema_hashfile})

		# Skip regeneration of the patches if hashes are the same
		if [ "$current_hash" == "$last_hash" ]; then
			printf "Current series already applies, skipping patch regeneration.\n"
			update_patches=false
			return
		fi
	fi

	# Save current hash for successive comparison
	echo $current_hash > $schema_hashfile

	# Reset the patches
	for patch in $PATCHES; do
		rm -f schema-patches/$patch
		sed -i -e "/$patch/d" schema-patches/series
	done
}

teardown() {
	make -f generate.mk clean
}

patch_by_script() {
	local patch=$1
	local script=$2

	quilt new $patch
	quilt add ${BUILD_DIRECTORY}/openapi/*.yaml

	python3 $script ${BUILD_DIRECTORY}/openapi

	quilt refresh
}

setup
if [ "$update_patches" = true ]; then
	for ((i = 0; i < ${#PATCHES[@]}; i++)); do
		patch_by_script ${PATCHES[$i]} ${SCRIPTS[$i]}
	done
fi
teardown
