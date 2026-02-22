#!/bin/bash

# WalkieTalkie - Basic Functionality Test Script
# This script performs basic verification of the app's core functionality

echo "🧪 WalkieTalkie - Basic Functionality Test"
echo "=========================================="
echo ""

# Test 1: Check if all required files exist
echo "✅ Test 1: Checking required files..."
missing_files=0

required_files=(
    "app/src/main/java/com/limemarmalade/walkietalkie/MainActivity.kt"
    "app/src/main/java/com/limemarmalade/walkietalkie/MainViewModel.kt"
    "app/src/main/java/com/limemarmalade/walkietalkie/WalkieTalkieService.kt"
    "app/src/main/java/com/limemarmalade/walkietalkie/Audio.kt"
    "app/src/main/java/com/limemarmalade/walkietalkie/Server.kt"
    "app/src/main/java/com/limemarmalade/walkietalkie/Client.kt"
    "app/src/main/java/com/limemarmalade/walkietalkie/billing/BillingManager.kt"
    "app/src/main/java/com/limemarmalade/walkietalkie/licensing/LicenseChecker.kt"
    "app/src/main/res/raw/privacy_policy.html"
    "app/src/main/res/raw/terms_of_service.html"
)

for file in "${required_files[@]}"; do
    if [ ! -f "$file" ]; then
        echo "❌ Missing file: $file"
        missing_files=$((missing_files + 1))
    fi
done

if [ $missing_files -eq 0 ]; then
    echo "✅ All required files present"
else
    echo "❌ $missing_files required files missing"
fi

echo ""

# Test 2: Check build configuration
echo "✅ Test 2: Checking build configuration..."
if [ -f "app/build.gradle.kts" ]; then
    echo "✅ Build configuration file exists"
    
    # Check for required dependencies
    if grep -q "billing" app/build.gradle.kts; then
        echo "✅ Billing dependency found"
    else
        echo "❌ Billing dependency missing"
    fi
    
    if grep -q "compose" app/build.gradle.kts; then
        echo "✅ Compose dependency found"
    else
        echo "❌ Compose dependency missing"
    fi
else
    echo "❌ Build configuration file missing"
fi

echo ""

# Test 3: Check manifest permissions
echo "✅ Test 3: Checking manifest permissions..."
if [ -f "app/src/main/AndroidManifest.xml" ]; then
    echo "✅ Manifest file exists"
    
    if grep -q "RECORD_AUDIO" app/src/main/AndroidManifest.xml; then
        echo "✅ Microphone permission found"
    else
        echo "❌ Microphone permission missing"
    fi
    
    if grep -q "FOREGROUND_SERVICE" app/src/main/AndroidManifest.xml; then
        echo "✅ Foreground service permission found"
    else
        echo "❌ Foreground service permission missing"
    fi
else
    echo "❌ Manifest file missing"
fi

echo ""

# Test 4: Check privacy policy and terms
echo "✅ Test 4: Checking legal documents..."
if [ -f "app/src/main/res/raw/privacy_policy.html" ] && [ -f "app/src/main/res/raw/terms_of_service.html" ]; then
    echo "✅ Privacy policy and terms of service present"
    
    # Check file sizes
    privacy_size=$(wc -c < "app/src/main/res/raw/privacy_policy.html")
    terms_size=$(wc -c < "app/src/main/res/raw/terms_of_service.html")
    
    if [ $privacy_size -gt 1000 ] && [ $terms_size -gt 1000 ]; then
        echo "✅ Legal documents have substantial content"
    else
        echo "⚠️  Legal documents may need more content"
    fi
else
    echo "❌ Legal documents missing"
fi

echo ""

# Test 5: Check store listing assets
echo "✅ Test 5: Checking store listing assets..."
if [ -d "walkietalkie-assets" ]; then
    echo "✅ Store listing assets directory exists"
    
    # Check for required asset types
    if [ -d "walkietalkie-assets/icons" ] && [ -d "walkietalkie-assets/graphics" ] && \
       [ -d "walkietalkie-assets/screenshots" ] && [ -d "walkietalkie-assets/text" ]; then
        echo "✅ All asset categories present"
        
        # Count assets
        icon_count=$(ls walkietalkie-assets/icons/ 2>/dev/null | wc -l)
        graphic_count=$(ls walkietalkie-assets/graphics/ 2>/dev/null | wc -l)
        screenshot_count=$(ls walkietalkie-assets/screenshots/ 2>/dev/null | wc -l)
        text_count=$(ls walkietalkie-assets/text/ 2>/dev/null | wc -l)
        
        echo "📊 Asset counts:"
        echo "   Icons: $icon_count"
        echo "   Graphics: $graphic_count"
        echo "   Screenshots: $screenshot_count"
        echo "   Text files: $text_count"
    else
        echo "❌ Some asset categories missing"
    fi
else
    echo "❌ Store listing assets directory missing"
fi

echo ""

# Test 6: Attempt to build the project
echo "✅ Test 6: Testing project build..."
if [ -f "gradlew" ]; then
    echo "✅ Gradle wrapper present"
    
    # Try to assemble debug build
    echo "Building project (this may take a moment)..."
    ./gradlew assembleDebug --console=plain -q
    
    if [ $? -eq 0 ]; then
        echo "✅ Project builds successfully"
    else
        echo "❌ Project build failed"
    fi
else
    echo "❌ Gradle wrapper missing"
fi

echo ""
echo "=========================================="
echo "📊 Test Summary"
echo "=========================================="
echo ""
echo "Basic functionality tests completed."
echo "For comprehensive testing, run the full test suite."
echo ""
echo "Next steps:"
echo "1. Run unit tests: ./gradlew test"
echo "2. Run UI tests: ./gradlew connectedAndroidTest"
echo "3. Manual testing on physical devices"
echo "4. Performance profiling"
echo ""

exit 0