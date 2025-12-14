// ============================================================
// MONGODB SCHEMA INITIALIZATION
// ============================================================
// Service: Product Service, Support Services
// Database: product_db
// ============================================================
// Run this script in MongoDB shell or via mongosh
// mongosh -u admin -p password --authenticationDatabase admin < 01-init-collections.js
// ============================================================

// Switch to product database
db = db.getSiblingDB('product_db');

// ============================================================
// COLLECTION: books
// ============================================================
// Main product catalog for books

db.createCollection("books", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["title", "isbn", "slug", "price", "status"],
            properties: {
                title: {
                    bsonType: "string",
                    description: "Book title - required"
                },
                isbn: {
                    bsonType: "string",
                    description: "ISBN number - required and unique"
                },
                slug: {
                    bsonType: "string",
                    description: "URL-friendly slug - required and unique"
                },
                price: {
                    bsonType: "object",
                    required: ["base"],
                    properties: {
                        base: { bsonType: "number", minimum: 0 },
                        sale: { bsonType: "number", minimum: 0 }
                    }
                },
                authors: {
                    bsonType: "array",
                    items: {
                        bsonType: "object",
                        properties: {
                            id: { bsonType: "int" },
                            name: { bsonType: "string" }
                        }
                    }
                },
                publisher: {
                    bsonType: "object",
                    properties: {
                        id: { bsonType: "int" },
                        name: { bsonType: "string" }
                    }
                },
                category: {
                    bsonType: "object",
                    properties: {
                        id: { bsonType: "int" },
                        name: { bsonType: "string" },
                        slug: { bsonType: "string" }
                    }
                },
                images: {
                    bsonType: "array",
                    items: {
                        bsonType: "object",
                        properties: {
                            url: { bsonType: "string" },
                            is_cover: { bsonType: "bool" }
                        }
                    }
                },
                attributes: {
                    bsonType: "object",
                    description: "Flexible attributes (pages, cover_type, language, etc.)"
                },
                description: { bsonType: "string" },
                short_description: { bsonType: "string" },
                status: {
                    enum: ["draft", "active", "inactive", "deleted"],
                    description: "Product status"
                },
                rating_average: { bsonType: "double", minimum: 0, maximum: 5 },
                review_count: { bsonType: "int", minimum: 0 },
                sold_count: { bsonType: "int", minimum: 0 },
                created_at: { bsonType: "date" },
                updated_at: { bsonType: "date" }
            }
        }
    }
});

// Indexes for books collection
db.books.createIndex({ "isbn": 1 }, { unique: true });
db.books.createIndex({ "slug": 1 }, { unique: true });
db.books.createIndex({ "title": "text", "description": "text" }); // Full-text search
db.books.createIndex({ "category.id": 1 });
db.books.createIndex({ "authors.id": 1 });
db.books.createIndex({ "publisher.id": 1 });
db.books.createIndex({ "status": 1 });
db.books.createIndex({ "price.sale": 1 });
db.books.createIndex({ "rating_average": -1 });
db.books.createIndex({ "sold_count": -1 });
db.books.createIndex({ "created_at": -1 });

print("✅ Created collection: books");

// ============================================================
// COLLECTION: categories
// ============================================================
// Product categories with hierarchical structure

db.createCollection("categories");

db.categories.createIndex({ "slug": 1 }, { unique: true });
db.categories.createIndex({ "parent_id": 1 });
db.categories.createIndex({ "display_order": 1 });
db.categories.createIndex({ "is_active": 1 });

print("✅ Created collection: categories");

// ============================================================
// COLLECTION: authors
// ============================================================
// Book authors

db.createCollection("authors");

db.authors.createIndex({ "slug": 1 }, { unique: true });
db.authors.createIndex({ "name": "text" });
db.authors.createIndex({ "is_active": 1 });

print("✅ Created collection: authors");

// ============================================================
// COLLECTION: publishers
// ============================================================
// Book publishers

db.createCollection("publishers");

db.publishers.createIndex({ "slug": 1 }, { unique: true });
db.publishers.createIndex({ "name": "text" });
db.publishers.createIndex({ "is_active": 1 });

print("✅ Created collection: publishers");

// ============================================================
// COLLECTION: reviews
// ============================================================
// Product reviews from customers

db.createCollection("reviews", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["book_id", "user_id", "rating"],
            properties: {
                book_id: { bsonType: "objectId" },
                user_id: { bsonType: "int" },
                order_id: { bsonType: "int" }, // Verify purchase
                rating: { bsonType: "int", minimum: 1, maximum: 5 },
                title: { bsonType: "string" },
                content: { bsonType: "string" },
                images: {
                    bsonType: "array",
                    items: { bsonType: "string" }
                },
                status: {
                    enum: ["pending", "approved", "rejected"],
                    description: "Review moderation status"
                },
                helpful_count: { bsonType: "int", minimum: 0 },
                reply: {
                    bsonType: "object",
                    properties: {
                        content: { bsonType: "string" },
                        replied_by: { bsonType: "int" },
                        replied_at: { bsonType: "date" }
                    }
                },
                created_at: { bsonType: "date" },
                updated_at: { bsonType: "date" }
            }
        }
    }
});

db.reviews.createIndex({ "book_id": 1 });
db.reviews.createIndex({ "user_id": 1 });
db.reviews.createIndex({ "order_id": 1 });
db.reviews.createIndex({ "status": 1 });
db.reviews.createIndex({ "rating": 1 });
db.reviews.createIndex({ "created_at": -1 });
db.reviews.createIndex({ "book_id": 1, "user_id": 1 }, { unique: true }); // One review per user per book

print("✅ Created collection: reviews");

// ============================================================
// COLLECTION: return_requests
// ============================================================
// Return/refund requests with evidence

db.createCollection("return_requests");

db.return_requests.createIndex({ "order_id": 1 });
db.return_requests.createIndex({ "user_id": 1 });
db.return_requests.createIndex({ "status": 1 });
db.return_requests.createIndex({ "created_at": -1 });

print("✅ Created collection: return_requests");

// ============================================================
// COLLECTION: banners
// ============================================================
// Homepage banners and promotional images

db.createCollection("banners");

db.banners.createIndex({ "position": 1 });
db.banners.createIndex({ "is_active": 1 });
db.banners.createIndex({ "display_order": 1 });
db.banners.createIndex({ "valid_from": 1, "valid_to": 1 });

print("✅ Created collection: banners");

// ============================================================
// COLLECTION: system_logs
// ============================================================
// Audit logs for system events

db.createCollection("system_logs", {
    capped: false // Consider capped collection for auto-cleanup
});

db.system_logs.createIndex({ "entity_type": 1, "entity_id": 1 });
db.system_logs.createIndex({ "action": 1 });
db.system_logs.createIndex({ "actor.id": 1 });
db.system_logs.createIndex({ "timestamp": -1 });
// TTL index to auto-delete logs older than 90 days
db.system_logs.createIndex({ "timestamp": 1 }, { expireAfterSeconds: 7776000 });

print("✅ Created collection: system_logs");

// ============================================================
// SAMPLE DATA: Categories
// ============================================================

db.categories.insertMany([
    {
        _id: 1,
        name: "Văn học",
        slug: "van-hoc",
        parent_id: null,
        icon: "book",
        description: "Sách văn học trong và ngoài nước",
        display_order: 1,
        is_active: true,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        _id: 2,
        name: "Kinh tế",
        slug: "kinh-te",
        parent_id: null,
        icon: "chart-line",
        description: "Sách kinh tế, kinh doanh",
        display_order: 2,
        is_active: true,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        _id: 3,
        name: "Công nghệ thông tin",
        slug: "cong-nghe-thong-tin",
        parent_id: null,
        icon: "laptop-code",
        description: "Sách IT, lập trình",
        display_order: 3,
        is_active: true,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        _id: 4,
        name: "Thiếu nhi",
        slug: "thieu-nhi",
        parent_id: null,
        icon: "child",
        description: "Sách dành cho thiếu nhi",
        display_order: 4,
        is_active: true,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        _id: 5,
        name: "Tâm lý - Kỹ năng sống",
        slug: "tam-ly-ky-nang-song",
        parent_id: null,
        icon: "brain",
        description: "Sách tâm lý, self-help",
        display_order: 5,
        is_active: true,
        created_at: new Date(),
        updated_at: new Date()
    }
]);

print("✅ Inserted sample categories");

// ============================================================
// SAMPLE DATA: Authors
// ============================================================

db.authors.insertMany([
    {
        _id: 1,
        name: "Nguyễn Nhật Ánh",
        slug: "nguyen-nhat-anh",
        bio: "Nhà văn Việt Nam nổi tiếng với các tác phẩm văn học thiếu nhi",
        avatar_url: null,
        is_active: true,
        book_count: 0,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        _id: 2,
        name: "Dale Carnegie",
        slug: "dale-carnegie",
        bio: "Tác giả người Mỹ, chuyên gia phát triển bản thân",
        avatar_url: null,
        is_active: true,
        book_count: 0,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        _id: 3,
        name: "Robert C. Martin",
        slug: "robert-c-martin",
        bio: "Uncle Bob - Chuyên gia phần mềm, tác giả Clean Code",
        avatar_url: null,
        is_active: true,
        book_count: 0,
        created_at: new Date(),
        updated_at: new Date()
    }
]);

print("✅ Inserted sample authors");

// ============================================================
// SAMPLE DATA: Publishers
// ============================================================

db.publishers.insertMany([
    {
        _id: 1,
        name: "NXB Trẻ",
        slug: "nxb-tre",
        address: "161B Lý Chính Thắng, Q.3, TP.HCM",
        website: "https://nxbtre.com.vn",
        is_active: true,
        book_count: 0,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        _id: 2,
        name: "NXB Kim Đồng",
        slug: "nxb-kim-dong",
        address: "55 Quang Trung, Hai Bà Trưng, Hà Nội",
        website: "https://nxbkimdong.com.vn",
        is_active: true,
        book_count: 0,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        _id: 3,
        name: "Alpha Books",
        slug: "alpha-books",
        address: "Hà Nội",
        website: "https://alphabooks.vn",
        is_active: true,
        book_count: 0,
        created_at: new Date(),
        updated_at: new Date()
    }
]);

print("✅ Inserted sample publishers");

// ============================================================
// SAMPLE DATA: Books
// ============================================================

db.books.insertMany([
    {
        title: "Clean Code",
        isbn: "978-0132350884",
        slug: "clean-code",
        price: {
            base: 450000,
            sale: 380000
        },
        authors: [
            { id: 3, name: "Robert C. Martin" }
        ],
        publisher: {
            id: 3,
            name: "Alpha Books"
        },
        category: {
            id: 3,
            name: "Công nghệ thông tin",
            slug: "cong-nghe-thong-tin"
        },
        images: [
            { url: "https://placeholder.com/clean-code-cover.jpg", is_cover: true }
        ],
        attributes: {
            pages: 464,
            cover_type: "Bìa mềm",
            language: "Tiếng Việt",
            publication_year: 2020,
            dimensions: "16 x 24 cm"
        },
        description: "Clean Code là cuốn sách kinh điển về nghệ thuật viết code sạch, dễ đọc và dễ bảo trì.",
        short_description: "Học cách viết code sạch từ Uncle Bob",
        status: "active",
        rating_average: 4.8,
        review_count: 120,
        sold_count: 500,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        title: "Đắc Nhân Tâm",
        isbn: "978-604-77-0000-1",
        slug: "dac-nhan-tam",
        price: {
            base: 108000,
            sale: 86000
        },
        authors: [
            { id: 2, name: "Dale Carnegie" }
        ],
        publisher: {
            id: 1,
            name: "NXB Trẻ"
        },
        category: {
            id: 5,
            name: "Tâm lý - Kỹ năng sống",
            slug: "tam-ly-ky-nang-song"
        },
        images: [
            { url: "https://placeholder.com/dac-nhan-tam-cover.jpg", is_cover: true }
        ],
        attributes: {
            pages: 320,
            cover_type: "Bìa cứng",
            language: "Tiếng Việt",
            publication_year: 2021,
            dimensions: "14.5 x 20.5 cm"
        },
        description: "Đắc Nhân Tâm là cuốn sách nổi tiếng nhất về nghệ thuật thu phục lòng người.",
        short_description: "Nghệ thuật thu phục lòng người",
        status: "active",
        rating_average: 4.9,
        review_count: 2500,
        sold_count: 10000,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        title: "Cho Tôi Xin Một Vé Đi Tuổi Thơ",
        isbn: "978-604-77-0000-2",
        slug: "cho-toi-xin-mot-ve-di-tuoi-tho",
        price: {
            base: 85000,
            sale: 72000
        },
        authors: [
            { id: 1, name: "Nguyễn Nhật Ánh" }
        ],
        publisher: {
            id: 1,
            name: "NXB Trẻ"
        },
        category: {
            id: 1,
            name: "Văn học",
            slug: "van-hoc"
        },
        images: [
            { url: "https://placeholder.com/cho-toi-xin-mot-ve-cover.jpg", is_cover: true }
        ],
        attributes: {
            pages: 220,
            cover_type: "Bìa mềm",
            language: "Tiếng Việt",
            publication_year: 2019,
            dimensions: "13 x 20 cm"
        },
        description: "Tác phẩm đưa người đọc trở về với những kỷ niệm tuổi thơ trong sáng.",
        short_description: "Hành trình trở về tuổi thơ",
        status: "active",
        rating_average: 4.7,
        review_count: 1800,
        sold_count: 8000,
        created_at: new Date(),
        updated_at: new Date()
    }
]);

print("✅ Inserted sample books");

print("\n============================================================");
print("✅ MongoDB initialization completed successfully!");
print("============================================================");
